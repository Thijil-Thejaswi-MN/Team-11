package com.team11.insurance.applications.dto;

import java.util.Locale;

import com.team11.insurance.applications.ApplicationStatus;

/** 201 body: the application id and its status after underwriting ("Approved" or "Referred"). */
public record SubmitApplicationResponse(Long applicationId, String status) {

    public static SubmitApplicationResponse of(Long applicationId, ApplicationStatus status) {
        String name = status.name();
        return new SubmitApplicationResponse(applicationId, name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT));
    }
}
