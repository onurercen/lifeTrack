package com.lifetrack.book.dto;

import java.time.LocalDateTime;

public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String description;
    private String userEmail;
    private LocalDateTime createdAt;

    public BookResponse() {
    }

    public BookResponse(Long id, String title, String author, String description, String userEmail, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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
