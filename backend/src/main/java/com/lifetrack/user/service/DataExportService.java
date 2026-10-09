package com.lifetrack.user.service;

import com.lifetrack.book.service.BookService;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.media.service.MediaService;
import com.lifetrack.run.service.RunService;
import com.lifetrack.user.dto.DataExport;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class DataExportService {

    private final UserRepository userRepository;
    private final RunService runService;
    private final BookService bookService;
    private final MediaService mediaService;
    private final Clock clock;

    public DataExportService(
        UserRepository userRepository,
        RunService runService,
        BookService bookService,
        MediaService mediaService,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.runService = runService;
        this.bookService = bookService;
        this.mediaService = mediaService;
        this.clock = clock;
    }

    // One read-only transaction so the lists are a consistent snapshot.
    @Transactional(readOnly = true)
    public DataExport export(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));
        return new DataExport(
            LocalDateTime.now(clock),
            new DataExport.Account(user.getName(), user.getEmail(), user.getCreatedAt()),
            runService.getAllRuns(email),
            bookService.getAllBooks(email),
            mediaService.getAllMedia(email)
        );
    }
}
