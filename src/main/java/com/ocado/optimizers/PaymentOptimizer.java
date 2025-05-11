package com.ocado.optimizers;

import com.ocado.models.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


public class PaymentOptimizer {
    private final List<Order> orders;
    private final Map<String, PaymentMethod> methods;
    private BigDecimal bestTotalDiscount;
    private List<PaymentAssignment> bestAssignment;

    public PaymentOptimizer(List<Order> orders, List<PaymentMethod> methodList) {
        this.orders = orders;
        this.methods = new HashMap<>();
        for (PaymentMethod pm : methodList) {
            if(pm instanceof  PointPayment) this.methods.put(pm.getId(), new PointPayment(pm.getId(),pm.getDiscount(),pm.getLimit()));
            else this.methods.put(pm.getId(), new CardPayment(pm.getId(),pm.getDiscount(),pm.getLimit()));
        }
        this.bestTotalDiscount = BigDecimal.ZERO;
        this.bestAssignment = new ArrayList<>();
    }

    public static class PaymentAssignment {
        private final Order order;
        private final PaymentMethod cardMethod;
        private final BigDecimal cardAmount;
        private final BigDecimal pointsAmount;
        private final BigDecimal discount;

        public PaymentAssignment(Order order,
                                 PaymentMethod cardMethod,
                                 BigDecimal cardAmount,
                                 BigDecimal pointsAmount,
                                 BigDecimal discount) {
            this.order = order;
            this.cardMethod = cardMethod;
            this.cardAmount = cardAmount.setScale(2, RoundingMode.HALF_EVEN);
            this.pointsAmount = pointsAmount.setScale(2, RoundingMode.HALF_EVEN);
            this.discount = discount.setScale(2, RoundingMode.HALF_EVEN);
        }
        public Order getOrder() { return order; }
        public PaymentMethod getCardMethod() { return cardMethod; }
        public BigDecimal getCardAmount() { return cardAmount; }
        public BigDecimal getPointsAmount() { return pointsAmount; }
        public BigDecimal getDiscount() { return discount; }
    }

    public static class Result {
        private final List<PaymentAssignment> assignments;
        private final BigDecimal totalDiscount;

        public Result(List<PaymentAssignment> assignments, BigDecimal totalDiscount) {
            this.assignments = assignments;
            this.totalDiscount = totalDiscount.setScale(2, RoundingMode.HALF_EVEN);
        }
        public List<PaymentAssignment> getAssignments() { return assignments; }
        public BigDecimal getTotalDiscount() { return totalDiscount; }
    }

    public Result optimize() {
        Map<String, BigDecimal> cardLimits = new HashMap<>();
        BigDecimal pointsLeft = BigDecimal.ZERO;
        for (PaymentMethod pm : methods.values()) {
            if (pm.getId().equals("PUNKTY")) {
                pointsLeft = pm.getLimit().setScale(2, RoundingMode.HALF_EVEN);
            } else {
                cardLimits.put(pm.getId(), pm.getLimit().setScale(2, RoundingMode.HALF_EVEN));
            }
        }
        bestTotalDiscount = BigDecimal.ZERO;
        bestAssignment.clear();
        backtrack(0, pointsLeft, cardLimits, BigDecimal.ZERO, new ArrayList<>());
        return new Result(bestAssignment, bestTotalDiscount);
    }

    private void backtrack(int idx,
                           BigDecimal pointsLeft,
                           Map<String, BigDecimal> cardLimitsLeft,
                           BigDecimal currentDiscount,
                           List<PaymentAssignment> currentAssign) {
        if (idx == orders.size()) {
            if (currentDiscount.compareTo(bestTotalDiscount) > 0) {
                bestTotalDiscount = currentDiscount;
                bestAssignment = new ArrayList<>(currentAssign);
            }
            return;
        }

        Order order = orders.get(idx);
        BigDecimal value = order.getValue().setScale(2, RoundingMode.HALF_EVEN);
        List<String> promos = order.getPromotions();
        PaymentMethod pointsMethod = methods.get("PUNKTY");

        // Case A: no card promotions
        if (promos == null || promos.isEmpty()) {
            // A1: full points
            if (pointsLeft.compareTo(value) >= 0) {
                BigDecimal ptRate = BigDecimal.valueOf(pointsMethod.getDiscount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
                BigDecimal disc = value.multiply(ptRate).setScale(2, RoundingMode.HALF_EVEN);
                currentAssign.add(new PaymentAssignment(order, null, BigDecimal.ZERO, value, disc));
                backtrack(idx+1, pointsLeft.subtract(value), cardLimitsLeft, currentDiscount.add(disc), currentAssign);
                currentAssign.remove(currentAssign.size()-1);
            }
            // A2: partial points ≥10% + any card
            BigDecimal tenPct = value.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_EVEN);
            if (pointsLeft.compareTo(tenPct) >= 0) {
                BigDecimal discountedTotal = value.multiply(BigDecimal.valueOf(0.90))
                        .setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal ptsUse = pointsLeft.min(discountedTotal).max(tenPct)
                        .setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal rem = discountedTotal.subtract(ptsUse).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal disc = value.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_EVEN);
                for (String cid : new ArrayList<>(cardLimitsLeft.keySet())) {
                    BigDecimal lim = cardLimitsLeft.get(cid);
                    if (lim.compareTo(rem) >= 0) {
                        cardLimitsLeft.put(cid, lim.subtract(rem));
                        currentAssign.add(new PaymentAssignment(order, methods.get(cid), rem, ptsUse, disc));
                        backtrack(idx+1, pointsLeft.subtract(ptsUse), cardLimitsLeft, currentDiscount.add(disc), currentAssign);
                        currentAssign.remove(currentAssign.size()-1);
                        cardLimitsLeft.put(cid, lim);
                    }
                }
            }
            // A3: full cards no discount
            BigDecimal toPay = value;
            Map<String, BigDecimal> savedLimits = new HashMap<>(cardLimitsLeft);
            List<PaymentAssignment> temp = new ArrayList<>(currentAssign);
            for (String cid : cardLimitsLeft.keySet()) {
                BigDecimal lim = cardLimitsLeft.get(cid);
                if (lim.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal use = toPay.min(lim).setScale(2, RoundingMode.HALF_EVEN);
                if (use.compareTo(BigDecimal.ZERO) > 0) {
                    cardLimitsLeft.put(cid, lim.subtract(use));
                    temp.add(new PaymentAssignment(order, methods.get(cid), use, BigDecimal.ZERO, BigDecimal.ZERO));
                    toPay = toPay.subtract(use);
                }
                if (toPay.compareTo(BigDecimal.ZERO)==0) break;
            }
            if (toPay.compareTo(BigDecimal.ZERO)==0) {
                currentAssign.addAll(temp.subList(currentAssign.size(), temp.size()));
                backtrack(idx+1, pointsLeft, cardLimitsLeft, currentDiscount, currentAssign);
                for (int i = currentAssign.size(); i < temp.size(); i++) currentAssign.remove(currentAssign.size()-1);
            }
            cardLimitsLeft.clear(); cardLimitsLeft.putAll(savedLimits);
            return;
        }

        // Case B: orders with card promotions
        // B1: full payment with each promo card
        for (String promoId : promos) {
            PaymentMethod card = methods.get(promoId);
            if (card == null) continue;
            BigDecimal rate = BigDecimal.valueOf(card.getDiscount());
            BigDecimal disc = value.multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
            BigDecimal pay = value.subtract(disc).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal lim = cardLimitsLeft.getOrDefault(promoId, BigDecimal.ZERO);
            if (lim.compareTo(pay) >= 0) {
                cardLimitsLeft.put(promoId, lim.subtract(pay));
                currentAssign.add(new PaymentAssignment(order, card, pay, BigDecimal.ZERO, disc));
                backtrack(idx+1, pointsLeft, cardLimitsLeft, currentDiscount.add(disc), currentAssign);
                currentAssign.remove(currentAssign.size()-1);
                cardLimitsLeft.put(promoId, lim);
            }
        }
        // B2: partial points ≥10% + promo card
        if (pointsLeft.compareTo(value.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_EVEN)) >= 0) {
            BigDecimal tenPct = value.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal discountedTotal = value.multiply(BigDecimal.valueOf(0.90)).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal disc = tenPct.multiply(BigDecimal.valueOf(1)).multiply(BigDecimal.valueOf(0)) // maintain 10%
                    .setScale(2, RoundingMode.HALF_EVEN);
            for (String promoId : promos) {
                PaymentMethod card = methods.get(promoId);
                BigDecimal lim = cardLimitsLeft.getOrDefault(promoId, BigDecimal.ZERO);
                BigDecimal ptsUse = pointsLeft.min(discountedTotal).max(tenPct)
                        .setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal rem = discountedTotal.subtract(ptsUse).setScale(2, RoundingMode.HALF_EVEN);
                if (lim.compareTo(rem) >= 0) {
                    cardLimitsLeft.put(promoId, lim.subtract(rem));
                    currentAssign.add(new PaymentAssignment(order, card, rem, ptsUse, value.multiply(BigDecimal.valueOf(0.10))
                            .setScale(2, RoundingMode.HALF_EVEN)));
                    backtrack(idx+1, pointsLeft.subtract(ptsUse), cardLimitsLeft, currentDiscount.add(value.multiply(BigDecimal.valueOf(0.10))
                            .setScale(2, RoundingMode.HALF_EVEN)), currentAssign);
                    currentAssign.remove(currentAssign.size()-1);
                    cardLimitsLeft.put(promoId, lim);
                }
            }
        }
        // B3: full points payment
        if (pointsLeft.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratePts = BigDecimal.valueOf(pointsMethod.getDiscount());
            BigDecimal disc = value.multiply(ratePts)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
            BigDecimal pay = value.subtract(disc).setScale(2, RoundingMode.HALF_EVEN);
            if (pointsLeft.compareTo(pay) >= 0) {
                currentAssign.add(new PaymentAssignment(order, null, BigDecimal.ZERO, pay, disc));
                backtrack(idx+1, pointsLeft.subtract(pay), cardLimitsLeft, currentDiscount.add(disc), currentAssign);
                currentAssign.remove(currentAssign.size()-1);
            }
        }
    }
}