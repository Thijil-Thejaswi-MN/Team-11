package com.team11.insurance.plans.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Paginated wrapper for public GET /plans responses (Contract 02 §4.1).
 */
public class PlanListResponse {

    @JsonProperty("items")
    private List<PlanResponse> items = new ArrayList<>();

    @JsonProperty("total")
    private long total;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    public PlanListResponse() {
    }

    public PlanListResponse(List<PlanResponse> items, long total, int page, int size) {
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<PlanResponse> getItems() {
        return items;
    }

    public void setItems(List<PlanResponse> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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
}
