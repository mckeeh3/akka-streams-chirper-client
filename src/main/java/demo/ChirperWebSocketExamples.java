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
import akka.stream.javadsl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class ChirperWebSocketExamples {
    private final ActorSystem actorSystem = ActorSystem.create();
    private final ActorMaterializer actorMaterializer = ActorMaterializer.create(actorSystem);
    private final Http http = Http.get(actorSystem);

    public static void main(String[] args) throws IOException {
        new ChirperWebSocketExamples().run();
    }

    private void run() throws IOException {
        userActivityHistory("lela");
        userActivityHistory("obiWan");

        userActivityLiveStream("luke");
        userActivityLiveStream("han");

        System.out.println("Streaming history and live activity. Hit Enter to stop...");
        System.in.read();
        System.out.println("Stopping actor system...");
        actorSystem.terminate();
    }

    private void userActivityHistory(String userId) {
        final Source<Message, NotUsed> source =
                Source.single(TextMessage.create(""));

        final Sink<Message, CompletionStage<Done>> sink =
                Sink.foreach(message -> System.out.println(String.format("History(%s): %s", userId, message.asTextMessage().getStrictText())));

        final Flow<Message, Message, CompletionStage<Done>> flow =
                Flow.fromSinkAndSourceMat(sink, source, Keep.left());

        final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<Done>> pair =
                http.singleWebSocketRequest(
                        WebSocketRequest.create(String.format("ws://localhost:9000/api/activity/%s/history", userId)),
                        flow,
                        actorMaterializer
                );

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

        connected.thenAccept(done -> System.out.println(String.format("Connected to history stream for user %s", userId)));
        closed.thenAccept(done -> System.out.println(String.format("Closed history stream for user %s", userId)));
    }

    private void userActivityLiveStream(String userId) {
        final Source<Message, NotUsed> source = Source.single(TextMessage.create(""));

        final Sink<Entity.Chirp, CompletionStage<Done>> sink =
                Sink.foreach(chirp -> System.out.println(String.format("Live(%s): %s", userId, chirp)));

        final WebSocketRequest webSocketRequest = WebSocketRequest.create(String.format("ws://localhost:9000/api/activity/%s/live", userId));
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
        closed.thenAccept(done -> System.out.println(String.format("Close live stream for user %s", userId)));
    }

    private static Flow<Message, Entity.Chirp, NotUsed> messageToChirpFlow() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader readChirp = objectMapper.readerFor(Entity.Chirp.class);

        final Flow<Message, String, NotUsed> messageToString = Flow.of(Message.class).map(message -> message.asTextMessage().getStrictText());
        final Flow<String, Entity.Chirp, NotUsed> jsonToChirp = Flow.of(String.class).map(readChirp::<Entity.Chirp>readValue);

        return messageToString.via(jsonToChirp);
    }
}
