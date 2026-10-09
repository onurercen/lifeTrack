package com.lifetrack.auth.repository;

import com.lifetrack.auth.entity.EmailCode;
import com.lifetrack.auth.entity.EmailCodePurpose;
import com.lifetrack.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailCodeRepository extends JpaRepository<EmailCode, Long> {

    Optional<EmailCode> findByUserAndPurpose(User user, EmailCodePurpose purpose);
}
