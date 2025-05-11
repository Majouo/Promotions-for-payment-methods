package com.ocado.parsers.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.ocado.models.Order;
import com.sun.source.tree.WhileLoopTree;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class OrdersJSONParser {

    public static List<Order> parseOrders(File jsonFile) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(jsonFile)) {
            List<Order> result = new ArrayList<>();

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected JSON array");
            }

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                Order o = new Order();

                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();

                    switch (fieldName) {
                        case "id":
                            o.setId(parser.getText());
                            break;
                        case "value":
                            o.setValue(new BigDecimal(parser.getText()).setScale(2, RoundingMode.HALF_EVEN));
                            break;
                        case "promotions":
                            List<String> promos = new ArrayList<>();
                            if (parser.currentToken() == JsonToken.START_ARRAY) {
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    promos.add(parser.getText());
                                }
                            } else {
                                promos.add(parser.getText());
                            }
                            o.setPromotions(promos);
                            break;
                        default:
                            parser.skipChildren();
                    }
                }
                result.add(o);
            }
            return result;
        }
    }

}
