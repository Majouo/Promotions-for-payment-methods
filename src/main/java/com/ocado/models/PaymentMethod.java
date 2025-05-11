package com.ocado.models;

import java.math.BigDecimal;

public abstract class PaymentMethod {
    private String id;
    private int discount;
    private BigDecimal limit;
    private BigDecimal remainingLimit;
    public PaymentMethod() {
    }

    public PaymentMethod(String id, int discount, BigDecimal limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
        this.remainingLimit = limit;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getDiscount() {
        return discount;
    }
    public void setDiscount(int discount) {
        this.discount = discount;
    }
    public BigDecimal getLimit() {
        return limit;
    }
    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }
    public BigDecimal getRemainingLimit() {return remainingLimit;}
    public void setRemainingLimit(BigDecimal remainingLimit) { this.remainingLimit = remainingLimit;}
}
