package com.ocado.models;

import java.math.BigDecimal;

public class CardPayment extends PaymentMethod{
    public CardPayment(String id, int discount, BigDecimal limit) {
        super(id,discount,limit);
    }
    public  CardPayment(){
        super();
    }
}
