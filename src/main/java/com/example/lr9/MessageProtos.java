package com.example.lr9;

public class MessageProtos {

    public static class ChatMessage {
        private String type;
        private String sender;
        private String receiver;
        private String content;
        private long timestamp;

        public static class Builder {
            private ChatMessage message = new ChatMessage();

            public Builder setType(String type) {
                message.type = type;
                return this;
            }

            public Builder setSender(String sender) {
                message.sender = sender;
                return this;
            }

            public Builder setReceiver(String receiver) {
                message.receiver = receiver;
                return this;
            }

            public Builder setContent(String content) {
                message.content = content;
                return this;
            }

            public Builder setTimestamp(long timestamp) {
                message.timestamp = timestamp;
                return this;
            }

            public ChatMessage build() {
                return message;
            }
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public String getType() {
            return type;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "ChatMessage{" +
                    "type='" + type + '\'' +
                    ", content='" + content + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}