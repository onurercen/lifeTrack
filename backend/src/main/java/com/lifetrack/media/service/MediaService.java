package com.lifetrack.media.service;

import com.lifetrack.common.exception.ApiException;
import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.media.entity.Media;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import static com.lifetrack.common.util.Strings.trimToNull;

import java.util.List;

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
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Media media = new Media();
        applyRequest(media, request);
        media.setUser(user);

        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    public MediaResponse updateMedia(Long id, CreateMediaRequest request, String email) {
        Media media = findOwnedMedia(id, email);
        applyRequest(media, request);
        return toResponse(mediaRepository.save(media));
    }

    public void deleteMedia(Long id, String email) {
        mediaRepository.delete(findOwnedMedia(id, email));
    }

    public List<MediaResponse> searchMedia(String query, String email) {
        String normalized = query == null ? "" : query.trim();
        List<Media> media = normalized.isEmpty()
            ? mediaRepository.findByUserEmailOrderByCreatedAtDesc(email)
            : mediaRepository.search(email, normalized);
        return media.stream().map(this::toResponse).toList();
    }

    // Another user's media is reported as missing, so ids can't be probed.
    private Media findOwnedMedia(Long id, String email) {
        return mediaRepository.findByIdAndUserEmail(id, email)
            .orElseThrow(() -> ApiException.notFound("Medya bulunamadı"));
    }

    private void applyRequest(Media media, CreateMediaRequest request) {
        media.setTitle(request.getTitle().trim());
        media.setType(request.getType().trim());
        media.setUrl(trimToNull(request.getUrl()));
        media.setDescription(trimToNull(request.getDescription()));
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
