package com.espresso.client.models;

import java.util.Objects;

/**
 * Represents a user in the chat application.
 */
public class User {
    private String userId;
    private String displayName;
    private boolean online;
    private String avatarInitials;

    /**
     * Creates a new User instance.
     * 
     * @param userId The unique identifier for the user
     * @param displayName The display name of the user
     */
    public User(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.online = false;
        
        // Generate avatar initials from display name
        if (displayName != null && !displayName.isEmpty()) {
            String[] parts = displayName.split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    initials.append(part.charAt(0));
                    if (initials.length() >= 2) break;
                }
            }
            this.avatarInitials = initials.toString().toUpperCase();
        } else {
            this.avatarInitials = userId.substring(0, Math.min(2, userId.length())).toUpperCase();
        }
    }

    /**
     * Gets the user ID.
     * 
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the display name.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     * 
     * @param displayName The new display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Checks if the user is online.
     * 
     * @return true if the user is online, false otherwise
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Sets the online status of the user.
     * 
     * @param online The new online status
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Gets the avatar initials for this user.
     * 
     * @return The avatar initials
     */
    public String getAvatarInitials() {
        return avatarInitials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return displayName + " (" + userId + ")";
    }
}