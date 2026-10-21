package com.team11.insurance.quotes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data Transfer Object for POST /api/v1/quotes/motor request payload (INS-F-005, SAD §4.3.3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MotorQuoteRequest {

    private Long planId;
    private String vehicleType;
    private String make;
    private String model;
    private Integer regYear;
    private LocalDate regDate;
    private String city;
    private BigDecimal idv;
    private List<String> addOns = new ArrayList<>();

    /**
     * Optional client-supplied premium parameter.
     * Note: Server calculations never trust client-supplied premiums; this field
     * exists strictly to verify and guard against client tampering.
     */
    private BigDecimal premium;

    public MotorQuoteRequest() {
    }

    public MotorQuoteRequest(Long planId, String vehicleType, String make, String model,
                             Integer regYear, String city, BigDecimal idv, List<String> addOns) {
        this.planId = planId;
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.regYear = regYear;
        this.city = city;
        this.idv = idv;
        this.addOns = addOns != null ? addOns : new ArrayList<>();
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getRegYear() {
        return regYear;
    }

    public void setRegYear(Integer regYear) {
        this.regYear = regYear;
    }

    public LocalDate getRegDate() {
        return regDate;
    }

    public void setRegDate(LocalDate regDate) {
        this.regDate = regDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getIdv() {
        return idv;
    }

    public void setIdv(BigDecimal idv) {
        this.idv = idv;
    }

    public List<String> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<String> addOns) {
        this.addOns = addOns != null ? addOns : new ArrayList<>();
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }
}
