package com.lifetrack.run.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateRunRequest {

    @NotNull(message = "Mesafe zorunludur")
    @DecimalMin(value = "0.1", message = "Mesafe 0.1 km'den az olamaz")
    private Double distanceKm;

    @NotNull(message = "Süre zorunludur")
    @Min(value = 1, message = "Süre en az 1 dakika olmalıdır")
    private Integer durationMinutes;

    @NotNull(message = "Kalori zorunludur")
    @Min(value = 1, message = "Kalori en az 1 olmalıdır")
    private Integer caloriesBurned;

    @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
    private String notes;

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(Integer caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
