package com.team11.insurance.quotes.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Individual member data for health quote calculations (API Contract §2.2, SAD §4.3.3).
 */
public class HealthMemberRequest {

    @JsonProperty("age")
    @NotNull(message = "Member age is required.")
    @Min(value = 18, message = "Member age must be at least 18.")
    @Max(value = 120, message = "Member age cannot exceed 120.")
    private Integer age;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("relation")
    private String relation;

    @JsonProperty("preExisting")
    @JsonAlias({"preExistingDisease", "hasPreExisting"})
    private Boolean preExisting;

    public HealthMemberRequest() {
    }

    public HealthMemberRequest(Integer age) {
        this.age = age;
    }

    public HealthMemberRequest(Integer age, Boolean preExisting) {
        this.age = age;
        this.preExisting = preExisting;
    }

    public HealthMemberRequest(Integer age, String gender, String relation) {
        this.age = age;
        this.gender = gender;
        this.relation = relation;
    }

    public HealthMemberRequest(Integer age, String gender, String relation, Boolean preExisting) {
        this.age = age;
        this.gender = gender;
        this.relation = relation;
        this.preExisting = preExisting;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Boolean getPreExisting() {
        return preExisting;
    }

    public boolean isPreExisting() {
        return Boolean.TRUE.equals(preExisting);
    }

    public void setPreExisting(Boolean preExisting) {
        this.preExisting = preExisting;
    }
}
