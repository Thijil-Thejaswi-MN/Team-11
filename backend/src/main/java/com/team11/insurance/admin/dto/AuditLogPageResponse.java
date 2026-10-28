package com.team11.insurance.admin.dto;

import java.util.Collections;
import java.util.List;

/**
 * Paginated container response for audit log queries (INS-F-018, SAD §5.2).
 */
public class AuditLogPageResponse {

    private List<AuditLogEntryResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public AuditLogPageResponse() {
        this.items = Collections.emptyList();
    }

    public AuditLogPageResponse(List<AuditLogEntryResponse> items, int page, int size, long totalElements, int totalPages) {
        this.items = items != null ? items : Collections.emptyList();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<AuditLogEntryResponse> getItems() {
        return items;
    }

    public void setItems(List<AuditLogEntryResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
