package com.lifetrack.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateMediaRequest {

    @NotBlank(message = "Medya başlığı zorunludur")
    private String title;

    @NotBlank(message = "Tür zorunludur")
    private String type;

    @NotBlank(message = "URL zorunludur")
    private String url;

    @NotBlank(message = "Açıklama zorunludur")
    @Size(min = 3, message = "Açıklama en az 3 karakter olmalıdır")
    private String description;

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
}
