package demo;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.scaladsl.model.HttpMethods;
import akka.stream.ActorMaterializer;
import akka.stream.Graph;
import akka.stream.SinkShape;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

class ChirperClientSimulator {
    private final ActorSystem actorSystem;
    private final ActorMaterializer actorMaterializer;

    private static Random random = new Random();
    private static List<String> messages = Arrays.asList(
            "Help me, Obi-Wan Kenobi. You’re my only hope.",
            "I find your lack of faith disturbing.",
            "It’s the ship that made the Kessel run in less than twelve parsecs.",
            "The Force will be with you. Always.",
            "Why, you stuck-up, half-witted, scruffy-looking nerf herder!",
            "Never tell me the odds!",
            "Do. Or do not. There is no try.",
            "I’ll never turn to the dark side.",
            "Now, young Skywalker, you will die.",
            "There’s always a bigger fish.",
            "You can’t stop the change, any more than you can stop the suns from setting.",
            "Fear is the path to the dark side.",
            "Well, if droids could think, there’d be none of us here, would there?",
            "We must keep our faith in the Republic.",
            "I’m just trying to make my way in the universe.",
            "The dark side of the Force is a pathway to many abilities some consider to be unnatural.",
            "Power! Unlimited power!",
            "So this is how liberty dies. With thunderous applause.",
            "To die for one’s people is a great sacrifice.",
            "You are the Chosen One.",
            "I am no Jedi.",
            "An object cannot make you good or evil.",
            "The garbage’ll do!",
            "You know, no matter how much we fought, I’ve always hated watching you leave.",
            "Oh, my dear friend. How I’ve missed you.",
            "I’m one with the Force. The Force is with me.",
            "Chewie, we’re home."
    );

    private enum Users {
        luke("Luke Skywalker"),
        han("Han Solo"),
        lela("Lela Organa"),
        obiWan("Obi-Wan Kenobi");

        private String name;

        Users(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) throws IOException {
        new ChirperClientSimulator().run();
    }

    private ChirperClientSimulator() {
        actorSystem = ActorSystem.create();
        actorMaterializer = ActorMaterializer.create(actorSystem);
    }

    private void run() throws IOException {
        Entity.User luke = new Entity.User(Users.luke.toString(), Users.luke.name);
        Entity.User han = new Entity.User(Users.han.toString(), Users.han.name);
        Entity.User lela = new Entity.User(Users.lela.toString(), Users.lela.name);
        Entity.User obiWan = new Entity.User(Users.obiWan.toString(), Users.obiWan.name);

        addUser(luke);
        addUser(han);
        addUser(lela);
        addUser(obiWan);

        addUserFriend(luke, new Entity.FriendId(han.getUserId()));
        addUserFriend(luke, new Entity.FriendId(lela.getUserId()));
        addUserFriend(luke, new Entity.FriendId(obiWan.getUserId()));

        addUserFriend(han, new Entity.FriendId(luke.getUserId()));
        addUserFriend(han, new Entity.FriendId(lela.getUserId()));
        addUserFriend(han, new Entity.FriendId(obiWan.getUserId()));

        addUserFriend(lela, new Entity.FriendId(luke.getUserId()));
        addUserFriend(lela, new Entity.FriendId(han.getUserId()));
        addUserFriend(lela, new Entity.FriendId(obiWan.getUserId()));

        addUserFriend(obiWan, new Entity.FriendId(luke.getUserId()));
        addUserFriend(obiWan, new Entity.FriendId(han.getUserId()));
        addUserFriend(obiWan, new Entity.FriendId(lela.getUserId()));

        getUser(luke.getUserId(), Sink.foreach(System.out::println));
        getUser(han.getUserId(), Sink.foreach(System.out::println));
        getUser(lela.getUserId(), Sink.foreach(System.out::println));
        getUser(obiWan.getUserId(), Sink.foreach(System.out::println));

        randomChirps();

        System.out.println("Adding users, friends, and chirps. Hit Enter to stop...");
        System.in.read();
        System.out.println("Stopping actor system...");
        actorSystem.terminate();
        System.exit(0);
    }

    private void addUser(Entity.User user) {
        final CompletionStage<HttpResponse> httpResponse = post("http://localhost:9000/api/users", user);

        httpResponse.thenApply(response -> {
            if (response.status().intValue() == 200) {
                System.out.printf("Status %d, added %s%n", response.status().intValue(), user);
                response.discardEntityBytes(actorMaterializer);
                return Done.getInstance();
            } else {
                throw new RuntimeException(String.format("%d - %s, %s", response.status().intValue(), response.status().reason(), user));
            }
        });
    }

    private void addUserFriend(Entity.User user, Entity.FriendId friendId) {
        final CompletionStage<HttpResponse> httpResponse = post(String.format("http://localhost:9000/api/users/%s/friends", user.getUserId()), friendId);

        httpResponse.thenApply(response -> {
            if (response.status().intValue() == 200) {
                System.out.printf("Status %d, user %s, added friend %s%n", response.status().intValue(), user, friendId);
                response.discardEntityBytes(actorMaterializer);
                return Done.getInstance();
            } else {
                throw new RuntimeException(String.format("%d - %s, %s", response.status().intValue(), response.status().reason(), friendId));
            }
        });
    }

    private void getUser(String userId, Graph<SinkShape<Entity.User>, ?> userSink) {
        final CompletionStage<HttpResponse> httpResponse = get(String.format("http://localhost:9000/api/users/%s", userId));

        final Flow<ByteString, String, NotUsed> bytesToString = JsonFraming.objectScanner(1024).map(ByteString::utf8String);
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader readUser = objectMapper.readerFor(Entity.User.class);
        final Flow<String, Entity.User, NotUsed> jsonToUser = Flow.of(String.class).map(readUser::<Entity.User>readValue);

        httpResponse.thenApply(response -> {
            if (response.status().intValue() == 200) {
                System.out.printf("Status %d, get %s%n", response.status().intValue(), userId);
                return response.entity()
                        .getDataBytes()
                        .via(bytesToString)
                        .via(jsonToUser)
                        .runWith(userSink, actorMaterializer);
            } else {
                throw new RuntimeException(String.format("%d - %s, %s", response.status().intValue(), response.status().reason(), userId));
            }
        });
    }

    private void chirp(Entity.Chirp chirp) {
        final CompletionStage<HttpResponse> httpResponse = post(String.format("http://localhost:9000/api/chirps/live/%s", chirp.getUserId()), chirp);

        httpResponse.thenApply(response -> {
            if (response.status().intValue() == 200) {
                System.out.printf("Status %d, post %s%n", response.status().intValue(), chirp);
                response.discardEntityBytes(actorMaterializer);
                return Done.getInstance();
            } else {
                throw new RuntimeException(String.format("%d - %s, %s", response.status().intValue(), response.status().reason(), chirp));
            }
        });
    }

    private CompletionStage<HttpResponse> get(String uri) {
        final HttpRequest request = HttpRequest.create()
                .withMethod(HttpMethods.GET())
                .withUri(uri);
        return Http.get(actorSystem).singleRequest(request);
    }

    private CompletionStage<HttpResponse> post(String uri, Object entity) {
        final HttpRequest request = HttpRequest.create()
                .withMethod(HttpMethods.POST())
                .withEntity(Entity.toJson(entity))
                .withUri(uri);
        return Http.get(actorSystem).singleRequest(request);
    }

    private void randomChirps() {
        final FiniteDuration tickInterval = FiniteDuration.create(1, TimeUnit.SECONDS);
        final Source<String, Cancellable> ticker = Source.tick(tickInterval, tickInterval, "tick");
        ticker.runForeach(t -> chirp(randomChirp()), actorMaterializer);
    }

    private static Entity.Chirp randomChirp() {
        return new Entity.Chirp(randomUserId(), randomMessage(), timestamp(), uuid());
    }

    private static String randomUserId() {
        int i = random.nextInt(Users.values().length);
        return Users.values()[i].toString();
    }

    private static String randomMessage() {
        int i = random.nextInt(messages.size());
        return messages.get(i);
    }

    private static String timestamp() {
        return Instant.now().toEpochMilli() + "";
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
