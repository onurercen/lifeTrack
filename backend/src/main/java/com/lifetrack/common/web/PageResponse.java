package com.lifetrack.common.web;

import com.lifetrack.common.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** One page of a list endpoint. Request the next page while [hasNext] is true. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, boolean hasNext) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    /** Validates the page and size query parameters. Ordering is fixed by each query. */
    public static Pageable request(int page, int size) {
        if (page < 0) {
            throw ApiException.invalidField("page", "Sayfa 0 veya daha büyük olmalıdır");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw ApiException.invalidField("size", "Sayfa boyutu 1 ile " + MAX_SIZE + " arasında olmalıdır");
        }
        return PageRequest.of(page, size);
    }
}
