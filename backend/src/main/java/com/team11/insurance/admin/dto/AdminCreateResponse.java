package com.team11.insurance.admin.dto;

/**
 * Generic response for admin creation endpoints returning { "id": ... } (SAD §4.3.6).
 */
public class AdminCreateResponse {

    private Long id;

    public AdminCreateResponse() {
    }

    public AdminCreateResponse(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
