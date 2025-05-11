package com.ocado;

import com.ocado.models.Order;
import com.ocado.models.PaymentMethod;
import com.ocado.optimizers.PaymentOptimizer;
import com.ocado.parsers.json.OrdersJSONParser;
import com.ocado.parsers.json.PaymentMethodsJSONParser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String ordersFilePath = null;
        String paymentMethodsFilePath = null;
        if(args.length!=2){
            printHelpAndExit();
        }
        ordersFilePath = args[0];
        paymentMethodsFilePath = args[1];
        File ordersJSON = new File(ordersFilePath);
        File paymentMethodsJSON = new File(paymentMethodsFilePath);

        try {
            List<Order> listOfOrders = OrdersJSONParser.parseOrders(ordersJSON);
            List<PaymentMethod> listOfPaymentMethods = PaymentMethodsJSONParser.parsePaymentMethods(paymentMethodsJSON);
            // Optimalization (backtracking)
            PaymentOptimizer optimizer = new PaymentOptimizer(listOfOrders, listOfPaymentMethods);
            PaymentOptimizer.Result result = optimizer.optimize();

            HashMap<Order,PaymentOptimizer.PaymentAssignment> trackFilter = new HashMap<>();
            for (var pa : result.getAssignments()) {
                trackFilter.put(pa.getOrder(),pa);
            }
            HashMap<String,BigDecimal> paymentTotalAmounts = new HashMap<>();
            paymentTotalAmounts.put("PUNKTY", BigDecimal.ZERO);
            for(var pa:trackFilter.values()){
                String card = pa.getCardMethod() != null ? pa.getCardMethod().getId() : "PUNKTY";
                if(pa.getCardMethod()!=null) {
                    if (paymentTotalAmounts.containsKey(card)) {
                        BigDecimal amt = paymentTotalAmounts.get(card);
                        amt = amt.add(pa.getCardAmount());
                        paymentTotalAmounts.put(card, amt);
                        if(pa.getPointsAmount().compareTo(BigDecimal.ZERO)>0){
                            amt = paymentTotalAmounts.get("PUNKTY");
                            amt = amt.add(pa.getPointsAmount());
                            paymentTotalAmounts.put("PUNKTY", amt);
                        }
                    } else {
                        paymentTotalAmounts.put(card, pa.getCardAmount());
                        if(pa.getPointsAmount().compareTo(BigDecimal.ZERO)>0){
                            BigDecimal amt = paymentTotalAmounts.get("PUNKTY");
                            amt = amt.add(pa.getPointsAmount());
                            paymentTotalAmounts.put("PUNKTY", amt);
                        }
                    }
                }else{
                    if (paymentTotalAmounts.containsKey("PUNKTY")) {
                        BigDecimal amt = paymentTotalAmounts.get("PUNKTY");
                        amt = amt.add(pa.getPointsAmount());
                        paymentTotalAmounts.put(card, amt);
                    } else {
                        paymentTotalAmounts.put("PUNKTY", pa.getCardAmount());
                    }
                }
            }
            paymentTotalAmounts.forEach((k,v)->{
                System.out.println(k+" "+v);
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void printHelpAndExit() {
        System.out.println("App usage: java -jar </file/path/to/orders> </file/path/to/payment/methods>");
        System.exit(0);
    }
}

