package com.nhom02.streamwire.model;

import java.io.Serializable;

public class CastMember implements Serializable {
    private String id;
    private String name;
    private String role;
    private String avatarUrl;

    public CastMember() {
    }

    public CastMember(String id, String name, String role, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}

