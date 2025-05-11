package com.ocado.models;

import java.math.BigDecimal;

public class PointPayment extends PaymentMethod{
    public PointPayment(String id, int discount, BigDecimal limit) {
        super(id,discount,limit);
    }
    public PointPayment(){
        super();
    }
}
