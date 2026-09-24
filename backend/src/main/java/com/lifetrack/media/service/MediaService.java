package com.lifetrack.media.service;

import com.lifetrack.common.exception.ApiException;
import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.media.entity.Media;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    public MediaService(MediaRepository mediaRepository, UserRepository userRepository) {
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    public MediaResponse createMedia(CreateMediaRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException("Kullanıcı bulunamadı"));

        Media media = new Media();
        media.setTitle(request.getTitle().trim());
        media.setType(request.getType().trim());
        media.setUrl(request.getUrl().trim());
        media.setDescription(request.getDescription().trim());
        media.setUser(user);

        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    public List<MediaResponse> getUserMedia(String email) {
        return mediaRepository.findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<MediaResponse> searchMedia(String query, String email) {
        String normalized = query == null ? "" : query.trim();
        return mediaRepository.findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .filter(media -> normalized.isEmpty()
                || media.getTitle().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT))
                || media.getType().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT))
                || media.getDescription().toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT)))
            .map(this::toResponse)
            .toList();
    }

    private MediaResponse toResponse(Media media) {
        return new MediaResponse(
            media.getId(),
            media.getTitle(),
            media.getType(),
            media.getUrl(),
            media.getDescription(),
            media.getUser().getEmail(),
            media.getCreatedAt()
        );
    }
}
