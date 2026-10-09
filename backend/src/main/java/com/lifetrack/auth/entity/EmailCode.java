package com.lifetrack.auth.entity;

import com.lifetrack.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A one-time code sent by e-mail. Reissuing replaces the code in the same row. */
@Entity
@Table(name = "email_codes")
public class EmailCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailCodePurpose purpose;

    @Column(nullable = false, length = 100)
    private String codeHash;

    /** Wrong guesses so far; the code is dropped once this reaches the limit. */
    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EmailCode() {
    }

    public EmailCode(User user, EmailCodePurpose purpose) {
        this.user = user;
        this.purpose = purpose;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public EmailCodePurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
