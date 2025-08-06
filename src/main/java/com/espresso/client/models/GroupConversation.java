package com.espresso.client.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a group conversation.
 */
public class GroupConversation {
    private Group group;
    private List<Message> messages;
    private long lastActivityTimestamp;
    private boolean unread;

    /**
     * Creates a new GroupConversation instance.
     * 
     * @param group The group this conversation belongs to
     */
    public GroupConversation(Group group) {
        this.group = group;
        this.messages = new ArrayList<>();
        this.lastActivityTimestamp = System.currentTimeMillis();
        this.unread = false;
    }

    /**
     * Gets the group this conversation belongs to.
     * 
     * @return The group
     */
    public Group getGroup() {
        return group;
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
        return "group_" + group.getGroupId();
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

    /**
     * Checks if a user is a member of this group conversation.
     * 
     * @param userId The ID of the user to check
     * @return true if the user is a member, false otherwise
     */
    public boolean isMember(String userId) {
        return group.isMember(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupConversation that = (GroupConversation) o;
        return Objects.equals(group.getGroupId(), that.group.getGroupId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(group.getGroupId());
    }

    @Override
    public String toString() {
        return "Group: " + group.getName();
    }
}