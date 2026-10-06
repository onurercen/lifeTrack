package com.lifetrack.media.service;

import com.lifetrack.common.exception.ApiException;
import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.media.entity.Media;
import com.lifetrack.media.entity.MediaStatus;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.lifetrack.common.util.Strings.trimToNull;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public MediaService(MediaRepository mediaRepository, UserRepository userRepository, Clock clock) {
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public MediaResponse createMedia(CreateMediaRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Media media = new Media();
        applyRequest(media, request, null);
        media.setUser(user);

        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    public MediaResponse updateMedia(Long id, CreateMediaRequest request, String email) {
        Media media = findOwnedMedia(id, email);
        applyRequest(media, request, media.getStatus());
        return toResponse(mediaRepository.save(media));
    }

    public void deleteMedia(Long id, String email) {
        mediaRepository.delete(findOwnedMedia(id, email));
    }

    public List<MediaResponse> searchMedia(String query, MediaStatus status, String email) {
        String normalized = query == null ? "" : query.trim();
        return mediaRepository.search(email, normalized, status).stream().map(this::toResponse).toList();
    }

    // Another user's media is reported as missing, so ids can't be probed.
    private Media findOwnedMedia(Long id, String email) {
        return mediaRepository.findByIdAndUserEmail(id, email)
            .orElseThrow(() -> ApiException.notFound("Medya bulunamadı"));
    }

    /** [previousStatus] is null for a new entry. */
    private void applyRequest(Media media, CreateMediaRequest request, MediaStatus previousStatus) {
        MediaStatus status = request.getStatus() != null ? request.getStatus() : media.getStatus();
        LocalDate today = LocalDate.now(clock);
        LocalDate finishedOn = null;
        if (status == MediaStatus.COMPLETED) {
            finishedOn = request.getFinishedOn();
            // Only stamp today when it was just completed, not on every edit of an old entry.
            if (finishedOn == null && status != previousStatus) {
                finishedOn = today;
            }
        }
        if (finishedOn != null && finishedOn.isAfter(today)) {
            throw ApiException.badRequest("Tarih gelecekte olamaz");
        }

        media.setTitle(request.getTitle().trim());
        media.setType(request.getType().trim());
        media.setUrl(trimToNull(request.getUrl()));
        media.setDescription(trimToNull(request.getDescription()));
        media.setStatus(status);
        media.setRating(request.getRating());
        media.setFinishedOn(finishedOn);
    }

    private MediaResponse toResponse(Media media) {
        return new MediaResponse(
            media.getId(),
            media.getTitle(),
            media.getType(),
            media.getUrl(),
            media.getDescription(),
            media.getStatus(),
            media.getRating(),
            media.getFinishedOn(),
            media.getUser().getEmail(),
            media.getCreatedAt()
        );
    }
}
