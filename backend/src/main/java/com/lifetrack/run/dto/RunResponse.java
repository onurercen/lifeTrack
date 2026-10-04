package com.lifetrack.run.dto;

import java.time.LocalDateTime;

public class RunResponse {

    private Long id;
    private Double distanceKm;
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private String notes;
    private String userEmail;
    private LocalDateTime runAt;
    private LocalDateTime createdAt;

    public RunResponse() {
    }

    public RunResponse(Long id, Double distanceKm, Integer durationMinutes, Integer caloriesBurned, String notes, String userEmail, LocalDateTime runAt, LocalDateTime createdAt) {
        this.id = id;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.notes = notes;
        this.userEmail = userEmail;
        this.runAt = runAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
