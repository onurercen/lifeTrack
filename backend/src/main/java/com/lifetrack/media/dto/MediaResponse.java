package com.lifetrack.media.dto;

import java.time.LocalDateTime;

public class MediaResponse {

    private Long id;
    private String title;
    private String type;
    private String url;
    private String description;
    private String userEmail;
    private LocalDateTime createdAt;

    public MediaResponse() {
    }

    public MediaResponse(Long id, String title, String type, String url, String description, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.url = url;
        this.description = description;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
