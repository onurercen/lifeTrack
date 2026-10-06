package com.lifetrack.book.dto;

import com.lifetrack.book.entity.BookStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateBookRequest {

    @NotBlank(message = "Kitap başlığı zorunludur")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    @NotBlank(message = "Yazar adı zorunludur")
    @Size(max = 255, message = "Yazar adı en fazla 255 karakter olabilir")
    private String author;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    // Omitted: WANT_TO_READ on create, unchanged on update.
    private BookStatus status;

    @Min(value = 1, message = "Sayfa sayısı en az 1 olmalıdır")
    @Max(value = 100000, message = "Sayfa sayısı çok büyük")
    private Integer pageCount;

    @Min(value = 0, message = "Okunan sayfa negatif olamaz")
    @Max(value = 100000, message = "Okunan sayfa çok büyük")
    private Integer currentPage;

    @Min(value = 1, message = "Puan 1 ile 5 arasında olmalıdır")
    @Max(value = 5, message = "Puan 1 ile 5 arasında olmalıdır")
    private Integer rating;

    private LocalDate startedOn;

    private LocalDate finishedOn;

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

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(LocalDate startedOn) {
        this.startedOn = startedOn;
    }

    public LocalDate getFinishedOn() {
        return finishedOn;
    }

    public void setFinishedOn(LocalDate finishedOn) {
        this.finishedOn = finishedOn;
    }
}
