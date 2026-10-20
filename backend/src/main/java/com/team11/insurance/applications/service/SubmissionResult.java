package com.team11.insurance.applications.service;

import com.team11.insurance.applications.ApplicationStatus;

/** What the submission service hands back: the controller turns Rejected into a 422 after the data is saved. */
public record SubmissionResult(Long applicationId, ApplicationStatus status, String reason) { }
