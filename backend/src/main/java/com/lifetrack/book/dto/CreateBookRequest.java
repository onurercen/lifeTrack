package com.lifetrack.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBookRequest {

    @NotBlank(message = "Kitap başlığı zorunludur")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    @NotBlank(message = "Yazar adı zorunludur")
    @Size(max = 255, message = "Yazar adı en fazla 255 karakter olabilir")
    private String author;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
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
