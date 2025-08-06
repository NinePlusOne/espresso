package com.espresso.client.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a direct message conversation between two users.
 */
public class DMConversation {
    private String userId1;
    private String userId2;
    private List<Message> messages;
    private long lastActivityTimestamp;
    private boolean unread;

    /**
     * Creates a new DMConversation instance.
     * 
     * @param userId1 The ID of the first user
     * @param userId2 The ID of the second user
     */
    public DMConversation(String userId1, String userId2) {
        // Ensure consistent ordering of user IDs
        if (userId1.compareTo(userId2) < 0) {
            this.userId1 = userId1;
            this.userId2 = userId2;
        } else {
            this.userId1 = userId2;
            this.userId2 = userId1;
        }
        
        this.messages = new ArrayList<>();
        this.lastActivityTimestamp = System.currentTimeMillis();
        this.unread = false;
    }

    /**
     * Gets the ID of the first user.
     * 
     * @return The ID of the first user
     */
    public String getUserId1() {
        return userId1;
    }

    /**
     * Gets the ID of the second user.
     * 
     * @return The ID of the second user
     */
    public String getUserId2() {
        return userId2;
    }

    /**
     * Gets the other user ID in the conversation.
     * 
     * @param currentUserId The ID of the current user
     * @return The ID of the other user
     */
    public String getOtherUserId(String currentUserId) {
        if (currentUserId.equals(userId1)) {
            return userId2;
        } else if (currentUserId.equals(userId2)) {
            return userId1;
        } else {
            throw new IllegalArgumentException("Current user is not part of this conversation");
        }
    }

    /**
     * Checks if a user is part of this conversation.
     * 
     * @param userId The ID of the user to check
     * @return true if the user is part of this conversation, false otherwise
     */
    public boolean hasUser(String userId) {
        return userId1.equals(userId) || userId2.equals(userId);
    }

    /**
     * Gets the list of messages in this conversation.
     * 
     * @return The list of messages
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Adds a message to this conversation.
     * 
     * @param message The message to add
     */
    public void addMessage(Message message) {
        messages.add(message);
        lastActivityTimestamp = System.currentTimeMillis();
        unread = true;
    }

    /**
     * Gets the timestamp of the last activity in this conversation.
     * 
     * @return The last activity timestamp
     */
    public long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    /**
     * Checks if this conversation has unread messages.
     * 
     * @return true if there are unread messages, false otherwise
     */
    public boolean isUnread() {
        return unread;
    }

    /**
     * Marks this conversation as read.
     */
    public void markAsRead() {
        unread = false;
    }

    /**
     * Gets the chat ID for this conversation.
     * 
     * @return The chat ID
     */
    public String getChatId() {
        return "dm_" + userId1 + "_" + userId2;
    }

    /**
     * Gets the last message in this conversation, or null if there are no messages.
     * 
     * @return The last message, or null
     */
    public Message getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DMConversation that = (DMConversation) o;
        return Objects.equals(userId1, that.userId1) && 
               Objects.equals(userId2, that.userId2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId1, userId2);
    }

    @Override
    public String toString() {
        return "DM: " + userId1 + " and " + userId2;
    }
}