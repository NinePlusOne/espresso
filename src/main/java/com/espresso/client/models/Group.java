package com.espresso.client.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a group in the chat application.
 */
public class Group {
    private String groupId;
    private String name;
    private String description;
    private Set<String> memberIds;
    private String ownerId;
    private long createdAt;

    /**
     * Creates a new Group instance.
     * 
     * @param groupId The unique identifier for the group
     * @param name The name of the group
     * @param description The description of the group
     * @param ownerId The ID of the group owner
     */
    public Group(String groupId, String name, String description, String ownerId) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.memberIds = new HashSet<>();
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        
        // Add the owner as a member
        this.memberIds.add(ownerId);
    }

    /**
     * Gets the group ID.
     * 
     * @return The group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the name of the group.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the group.
     * 
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the group.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the group.
     * 
     * @param description The new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the set of member IDs.
     * 
     * @return The set of member IDs
     */
    public Set<String> getMemberIds() {
        return new HashSet<>(memberIds);
    }

    /**
     * Gets the list of member IDs.
     * 
     * @return The list of member IDs
     */
    public List<String> getMemberIdsList() {
        return new ArrayList<>(memberIds);
    }

    /**
     * Adds a member to the group.
     * 
     * @param userId The ID of the user to add
     * @return true if the user was added, false if they were already a member
     */
    public boolean addMember(String userId) {
        return memberIds.add(userId);
    }

    /**
     * Removes a member from the group.
     * 
     * @param userId The ID of the user to remove
     * @return true if the user was removed, false if they weren't a member
     */
    public boolean removeMember(String userId) {
        if (userId.equals(ownerId)) {
            return false; // Cannot remove the owner
        }
        return memberIds.remove(userId);
    }

    /**
     * Checks if a user is a member of the group.
     * 
     * @param userId The ID of the user to check
     * @return true if the user is a member, false otherwise
     */
    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    /**
     * Gets the ID of the group owner.
     * 
     * @return The owner ID
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Checks if a user is the owner of the group.
     * 
     * @param userId The ID of the user to check
     * @return true if the user is the owner, false otherwise
     */
    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }

    /**
     * Gets the timestamp of when the group was created.
     * 
     * @return The creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the number of members in the group.
     * 
     * @return The number of members
     */
    public int getMemberCount() {
        return memberIds.size();
    }

    /**
     * Gets the chat ID for this group.
     * 
     * @return The chat ID
     */
    public String getChatId() {
        return "group_" + groupId;
    }

    @Override
    public String toString() {
        return name + " (" + memberIds.size() + " members)";
    }
}