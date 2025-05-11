package com.ocado.models;

import java.math.BigDecimal;

public class CardOption {
    public PaymentMethod card;
    public Order order;
    public BigDecimal discountValue;
    public BigDecimal requiredAmount;

    public CardOption(PaymentMethod c, Order o, BigDecimal d, BigDecimal req) {
        this.card = c;
        this.order = o;
        this.discountValue = d;
        this.requiredAmount = req;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }
}
