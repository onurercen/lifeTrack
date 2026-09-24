package com.lifetrack.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBookRequest {

    @NotBlank(message = "Kitap başlığı zorunludur")
    private String title;

    @NotBlank(message = "Yazar adı zorunludur")
    private String author;

    @NotBlank(message = "Açıklama zorunludur")
    @Size(min = 3, message = "Açıklama en az 3 karakter olmalıdır")
    private String description;

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
}
