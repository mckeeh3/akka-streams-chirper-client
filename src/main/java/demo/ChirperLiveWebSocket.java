package demo;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ChirperLiveWebSocket {
    private final ActorSystem actorSystem = ActorSystem.create();
    private final ActorMaterializer actorMaterializer = ActorMaterializer.create(actorSystem);
    private final Http http = Http.get(actorSystem);

    public static void main(String[] args) throws IOException {
        new ChirperLiveWebSocket().run();
    }

    private void run() throws IOException {
        userActivityLiveStream("luke");
        userActivityLiveStream("han");
        userActivityLiveStream("lela");
        userActivityLiveStream("obiWan");

        System.out.println("Streaming live activity. Hit Enter to stop...");
        System.in.read();
        System.out.println("Stopping actor system...");
        actorSystem.terminate();
        System.exit(0);
    }

    private void userActivityLiveStream(String userId) {
        final Sink<Message, CompletionStage<Done>> sink =
                messageToChirpFlow().toMat(
                        Sink.foreach(chirp -> System.out.println(String.format("Live(%s): %s", userId, chirp))), Keep.right()
                );

        final Flow<Message, Message, CompletableFuture<Optional<Message>>> flow =
                Flow.fromSinkAndSourceMat(
                        sink,
                        Source.maybe(),
                        Keep.right());

        final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletableFuture<Optional<Message>>> pair =
                http.singleWebSocketRequest(
                        WebSocketRequest.create(String.format("ws://localhost:9000/api/activity/%s/live", userId)),
                        flow,
                        actorMaterializer
                );

        final CompletionStage<WebSocketUpgradeResponse> upgradeResponseCompletionStage = pair.first();
        final CompletableFuture<Optional<Message>> closed = pair.second();

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

    private static Flow<Message, Entity.Chirp, NotUsed> messageToChirpFlow() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader readChirp = objectMapper.readerFor(Entity.Chirp.class);

        final Flow<Message, String, NotUsed> messageToString = Flow.of(Message.class).map(message -> message.asTextMessage().getStrictText());
        final Flow<String, Entity.Chirp, NotUsed> jsonToChirp = Flow.of(String.class).map(readChirp::<Entity.Chirp>readValue);

        return messageToString.via(jsonToChirp);
    }
}
