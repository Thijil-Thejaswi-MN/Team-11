package com.team11.insurance.common.api;

import java.util.Collections;
import java.util.List;

/**
 * Standard Paginated Envelope per SAD Section 4.3:
 * "Every list endpoint supports page and size."
 */
public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public PageResponse(List<T> items, int page, int size, long totalElements, int totalPages, boolean last) {
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}
