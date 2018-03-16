package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JacksonTest {
    @Test
    void jsonUser() throws IOException {
        List<String> friends = new ArrayList<>();
        friends.add("friend1");
        friends.add("friend2");
        friends.add("friend3");
        Entity.User user1 = new Entity.User("userId", "User Name", friends);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(user1);

        assertNotNull(json);
        assertTrue(json.contains("\"userId\":"));
        assertTrue(json.contains("\"name\":"));
        assertTrue(json.contains("\"friends\":"));

        Entity.User user2 = objectMapper.readValue(json, Entity.User.class);

        assertEquals(user1.getUserId(), user2.getUserId());
        assertEquals(user1.getName(), user2.getName());
        assertEquals(user1.getFriends(), user2.getFriends());
    }

    @Test
    void jsonFriendId() throws IOException {
        Entity.FriendId friendId1 = new Entity.FriendId("friendId");

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(friendId1);

        assertNotNull(json);
        assertTrue(json.contains("\"friendId\":"));

        Entity.FriendId friendId2 = objectMapper.readValue(json, Entity.FriendId.class);

        assertEquals(friendId1.getFriendId(), friendId2.getFriendId());
    }

    @Test
    void jsonChirp() throws IOException {
        Entity.Chirp chirp1 = new Entity.Chirp("userId", "message", "timestamp", "uuid");

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(chirp1);

        assertNotNull(json);
        assertTrue(json.contains("\"userId\":"));
        assertTrue(json.contains("\"message\":"));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"uuid\":"));

        Entity.Chirp chirp2 = objectMapper.readValue(json, Entity.Chirp.class);

        assertEquals(chirp1.getUserId(), chirp2.getUserId());
        assertEquals(chirp1.getMessage(), chirp2.getMessage());
        assertEquals(chirp1.getTimestamp(), chirp2.getTimestamp());
        assertEquals(chirp1.getUuid(), chirp2.getUuid());
    }
}
