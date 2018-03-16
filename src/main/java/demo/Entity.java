package demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

interface Entity {
    @JsonDeserialize
    class User {
        private final String userId;
        private final String name;
        private final List<String> friends;

        @JsonCreator
        User(@JsonProperty("userId") String userId,
             @JsonProperty("name") String name,
             @JsonProperty("friends") List<String> friends) {
            this.userId = userId;
            this.name = name;
            this.friends = friends;
        }

        User(String userId, String name) {
            this(userId, name, new ArrayList<>());
        }

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public List<String> getFriends() {
            return friends;
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), getUserId(), getName(), getFriends());
        }
    }

    @JsonDeserialize
    class FriendId {
        private final String friendId;

        @JsonCreator
        FriendId(@JsonProperty("friendId") String friendId) {
            this.friendId = friendId;
        }

        public String getFriendId() {
            return friendId;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), getFriendId());
        }
    }

    @JsonDeserialize
    class Chirp {
        private final String userId;
        private final String message;
        private final String timestamp;
        private final String uuid;

        @JsonCreator
        Chirp(@JsonProperty("userId") String userId,
              @JsonProperty("message") String message,
              @JsonProperty("timestamp") String timestamp,
              @JsonProperty("uuid") String uuid) {
            this.userId = userId;
            this.message = message;
            this.timestamp = timestamp;
            this.uuid = uuid;
        }

        public String getUserId() {
            return userId;
        }

        public String getMessage() {
            return message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getUuid() {
            return uuid;
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s, %s, %s]", getClass().getSimpleName(), userId, message, timestamp, uuid);
        }
    }

    static String toJson(Object entity) {
        try {
            return (new ObjectMapper().writeValueAsString(entity));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize " + entity);
        }
    }
}
