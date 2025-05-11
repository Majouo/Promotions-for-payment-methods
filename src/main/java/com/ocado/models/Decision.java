package com.ocado.models;

import java.math.BigDecimal;

public class Decision {
    public enum Type { CARD_FULL, PARTIAL_POINTS, FULL_POINTS }
    public Type type;
    public String methodId;       // karta lub "PUNKTY"
    public BigDecimal pointsUsed; // ile pkt
    public BigDecimal cashUsed;   // ile gotówki/karty
    public BigDecimal discountValue;

    // Konstruktorzy
    public Decision(Type type, String id, BigDecimal cashUsed, BigDecimal discount) {
        this(type, id, BigDecimal.ZERO, discount, null, cashUsed);
    }
    public Decision(Type type, String id, BigDecimal pointsUsed, BigDecimal discount,
                    String cardUsedId, BigDecimal cashUsed) {
        this.type = type;
        this.methodId = id;
        this.pointsUsed = pointsUsed;
        this.discountValue = discount;
        this.cashUsed = cashUsed;
    }
}