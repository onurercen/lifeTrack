package com.lifetrack.media.dto;

import com.lifetrack.common.util.Strings;
import com.lifetrack.media.entity.MediaStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public class CreateMediaRequest {

    @NotBlank(message = "Medya başlığı zorunludur")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    @NotBlank(message = "Tür zorunludur")
    @Size(max = 255, message = "Tür en fazla 255 karakter olabilir")
    private String type;

    @URL(message = "Geçerli bir bağlantı giriniz")
    @Size(max = 2048, message = "Bağlantı en fazla 2048 karakter olabilir")
    private String url;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    // Omitted: PLANNED on create, unchanged on update.
    private MediaStatus status;

    @Min(value = 1, message = "Puan 1 ile 5 arasında olmalıdır")
    @Max(value = 5, message = "Puan 1 ile 5 arasında olmalıdır")
    private Integer rating;

    private LocalDate finishedOn;

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

    // Normalised before validation so a blank link counts as "no link".
    public void setUrl(String url) {
        this.url = Strings.trimToNull(url);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDate getFinishedOn() {
        return finishedOn;
    }

    public void setFinishedOn(LocalDate finishedOn) {
        this.finishedOn = finishedOn;
    }
}
