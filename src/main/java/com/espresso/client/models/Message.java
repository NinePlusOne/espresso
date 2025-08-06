package com.espresso.client.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a message in the chat application.
 */
public class Message {
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private String chatId;
    private MessageType type;

    /**
     * Enum representing different types of messages.
     */
    public enum MessageType {
        TEXT,
        USER_JOINED,
        USER_LEFT,
        ERROR
    }

    /**
     * Creates a new Message instance.
     * 
     * @param messageId The unique identifier for the message
     * @param senderId The ID of the sender
     * @param senderName The name of the sender
     * @param content The content of the message
     * @param timestamp The timestamp when the message was sent
     * @param chatId The ID of the chat this message belongs to
     * @param type The type of the message
     */
    public Message(String messageId, String senderId, String senderName, String content, long timestamp, String chatId, MessageType type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.chatId = chatId;
        this.type = type;
    }

    /**
     * Creates a new text message.
     * 
     * @param senderId The ID of the sender
     * @param senderName The name of the sender
     * @param content The content of the message
     * @param chatId The ID of the chat this message belongs to
     * @return A new Message instance
     */
    public static Message createTextMessage(String senderId, String senderName, String content, String chatId) {
        return new Message(
            generateMessageId(),
            senderId,
            senderName,
            content,
            System.currentTimeMillis(),
            chatId,
            MessageType.TEXT
        );
    }

    /**
     * Creates a new system message for when a user joins a chat.
     * 
     * @param userId The ID of the user who joined
     * @param userName The name of the user who joined
     * @param chatId The ID of the chat the user joined
     * @return A new Message instance
     */
    public static Message createUserJoinedMessage(String userId, String userName, String chatId) {
        return new Message(
            generateMessageId(),
            userId,
            userName,
            userName + " joined the chat",
            System.currentTimeMillis(),
            chatId,
            MessageType.USER_JOINED
        );
    }

    /**
     * Creates a new system message for when a user leaves a chat.
     * 
     * @param userId The ID of the user who left
     * @param userName The name of the user who left
     * @param chatId The ID of the chat the user left
     * @return A new Message instance
     */
    public static Message createUserLeftMessage(String userId, String userName, String chatId) {
        return new Message(
            generateMessageId(),
            userId,
            userName,
            userName + " left the chat",
            System.currentTimeMillis(),
            chatId,
            MessageType.USER_LEFT
        );
    }

    /**
     * Creates a new error message.
     * 
     * @param errorContent The error content
     * @param chatId The ID of the chat where the error occurred
     * @return A new Message instance
     */
    public static Message createErrorMessage(String errorContent, String chatId) {
        return new Message(
            generateMessageId(),
            "system",
            "System",
            "Error: " + errorContent,
            System.currentTimeMillis(),
            chatId,
            MessageType.ERROR
        );
    }

    /**
     * Generates a unique message ID.
     * 
     * @return A unique message ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    /**
     * Gets the message ID.
     * 
     * @return The message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the sender ID.
     * 
     * @return The sender ID
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Gets the sender name.
     * 
     * @return The sender name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Gets the content of the message.
     * 
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the timestamp of when the message was sent.
     * 
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the formatted time string for display.
     * 
     * @return The formatted time string
     */
    public String getFormattedTime() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Gets the formatted date string for display.
     * 
     * @return The formatted date string
     */
    public String getFormattedDate() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    /**
     * Gets the chat ID.
     * 
     * @return The chat ID
     */
    public String getChatId() {
        return chatId;
    }

    /**
     * Gets the message type.
     * 
     * @return The message type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Checks if this message is from the system.
     * 
     * @return true if this is a system message, false otherwise
     */
    public boolean isSystemMessage() {
        return type == MessageType.USER_JOINED || 
               type == MessageType.USER_LEFT || 
               type == MessageType.ERROR;
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + senderName + ": " + content;
    }
}