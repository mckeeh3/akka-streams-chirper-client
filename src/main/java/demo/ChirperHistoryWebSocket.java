package demo;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.util.concurrent.CompletionStage;

public class ChirperHistoryWebSocket {
    private final ActorSystem actorSystem = ActorSystem.create();
    private final ActorMaterializer actorMaterializer = ActorMaterializer.create(actorSystem);
    private final Http http = Http.get(actorSystem);

    public static void main(String[] args) {
        new ChirperHistoryWebSocket().run();
    }

    private void run() {
        userActivityHistoryStream("luke");
        userActivityHistoryStream("han");
        userActivityHistoryStream("lela");
        userActivityHistoryStream("obiWan");
    }

    private void userActivityHistoryStream(String userId) {
        final Source<Message, NotUsed> source = Source.single(TextMessage.create(""));

        final Sink<Entity.Chirp, CompletionStage<Done>> sink =
                Sink.foreach(chirp -> System.out.println(String.format("History(%s): %s", userId, chirp)));

        final WebSocketRequest webSocketRequest = WebSocketRequest.create(String.format("ws://localhost:9000/api/activity/%s/history", userId));
        final Flow<Message, Message, CompletionStage<WebSocketUpgradeResponse>> webSocketClientFlow = http.webSocketClientFlow(webSocketRequest);

        final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<Done>> pair =
                source.viaMat(webSocketClientFlow, Keep.right())
                        .via(messageToChirpFlow())
                        .toMat(sink, Keep.both())
                        .run(actorMaterializer);

        final CompletionStage<WebSocketUpgradeResponse> upgradeResponseCompletionStage = pair.first();
        final CompletionStage<Done> closed = pair.second();

        final CompletionStage<Done> connected = upgradeResponseCompletionStage
                .thenApply(upgrade -> {
                    if (upgrade.response().status().equals(StatusCodes.SWITCHING_PROTOCOLS)) {
                        return Done.getInstance();
                    } else {
                        throw new RuntimeException("Connection failed: " + upgrade.response().status());
                    }
                });

        connected.thenAccept(done -> System.out.println(String.format("Connected to live stream for user %s", userId)));
        closed.thenAccept(done -> {
            System.out.println(String.format("Close live stream for user %s", userId));
            actorSystem.terminate();
            System.exit(0);
        });
    }

    private static Flow<Message, Entity.Chirp, NotUsed> messageToChirpFlow() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader readChirp = objectMapper.readerFor(Entity.Chirp.class);

        final Flow<Message, String, NotUsed> messageToString = Flow.of(Message.class).map(message -> message.asTextMessage().getStrictText());
        final Flow<String, Entity.Chirp, NotUsed> jsonToChirp = Flow.of(String.class).map(readChirp::<Entity.Chirp>readValue);

        return messageToString.via(jsonToChirp);
    }
}
