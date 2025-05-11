import com.ocado.models.CardPayment;
import com.ocado.models.Order;
import com.ocado.models.PaymentMethod;
import com.ocado.models.PointPayment;
import com.ocado.optimizers.PaymentOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentOptimizerTest {
    private List<Order> orders;
    private List<PaymentMethod> methods;

    @BeforeEach
    void setUp() {
        orders = new ArrayList<>();
        methods = new ArrayList<>();
    }

    @Test
    void fullCardPaymentTest() {
        orders.add(new Order("ORDER1", new BigDecimal("100.00"), List.of("CARD1")));
        methods.add(new PointPayment("PUNKTY", 0, new BigDecimal("0.00")));
        methods.add(new CardPayment("CARD1", 10, new BigDecimal("150.00")));

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        var result = optimizer.optimize();

        assertEquals(new BigDecimal("10.00"), result.getTotalDiscount());
        var pa = result.getAssignments().get(0);
        assertEquals("ORDER1", pa.getOrder().getId());
        assertEquals("CARD1", pa.getCardMethod().getId());
        assertEquals(new BigDecimal("90.00"), pa.getCardAmount());
    }

    @Test
    void fullPointsPaymentTest() {
        orders.add(new Order("ORDER1", new BigDecimal("80.00"), Collections.emptyList()));
        methods.add(new PointPayment("PUNKTY", 20, new BigDecimal("80.00")));

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        var result = optimizer.optimize();

        assertEquals(new BigDecimal("16.00"), result.getTotalDiscount());  // 20% of 80
        var pa = result.getAssignments().get(0);
        assertNull(pa.getCardMethod());
    }

    @Test
    void partialPointsAndCardTest() {
        orders.add(new Order("ORDER1", new BigDecimal("100.00"), null));
        methods.add(new PointPayment("PUNKTY", 15, new BigDecimal("20.00")));
        methods.add(new CardPayment("CARD1", 0, new BigDecimal("100.00")));

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        var result = optimizer.optimize();

        assertEquals(new BigDecimal("10.00"), result.getTotalDiscount());  // 10% partial
        var pa = result.getAssignments().get(0);
        assertEquals(pa.getCardMethod().getId(),"CARD1");
    }

    @Test
    void chooseBestPromotionTest() {
        orders.add(new Order("ORDER1", new BigDecimal("200.00"), List.of("A", "B")));
        methods.add(new PointPayment("PUNKTY", 0, new BigDecimal("0.00")));
        methods.add(new CardPayment("A", 5, new BigDecimal("200.00")));
        methods.add(new CardPayment("B", 10, new BigDecimal("200.00")));

        PaymentOptimizer optimizer = new PaymentOptimizer(orders, methods);
        var result = optimizer.optimize();

        assertEquals(new BigDecimal("20.00"), result.getTotalDiscount());
        var pa = result.getAssignments().get(0);
        assertEquals("B", pa.getCardMethod().getId());
    }

}
