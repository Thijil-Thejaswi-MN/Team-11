package com.team11.insurance.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * System Admin request DTO for creating insurers (SAD §4.3.6, API Contract §5.2).
 */
public class CreateInsurerRequest {

    @NotBlank(message = "name is required")
    private String name;

    public CreateInsurerRequest() {
    }

    public CreateInsurerRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
