package com.ocado.parsers.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.ocado.models.CardPayment;
import com.ocado.models.PaymentMethod;
import com.ocado.models.PointPayment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PaymentMethodsJSONParser {
    public static List<PaymentMethod> parsePaymentMethods(File jsonFile) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(jsonFile)) {
            List<PaymentMethod> result = new ArrayList<>();
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected JSON format");
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                String fieldName;
                PaymentMethod pm=null;
                if((fieldName = parser.nextFieldName()) != null){
                    if(fieldName.equals("id")){
                        String id = parser.nextTextValue();
                        if (Objects.equals(id, "PUNKTY")) pm = new PointPayment();
                        else pm = new CardPayment();
                        pm.setId(id);
                        while((fieldName = parser.nextFieldName()) != null){
                            switch (fieldName) {
                                case "discount":
                                    pm.setDiscount(Integer.parseInt(parser.nextTextValue()));
                                    break;
                                case "limit":
                                    pm.setLimit(new BigDecimal(parser.nextTextValue()).setScale(2, RoundingMode.HALF_EVEN));
                                    pm.setRemainingLimit(pm.getLimit());
                                    break;
                                default:
                                    parser.skipChildren();
                            }
                        }
                    }
                }
                if(pm!=null)
                result.add(pm);
            }
            return result;
        }
    }
}
