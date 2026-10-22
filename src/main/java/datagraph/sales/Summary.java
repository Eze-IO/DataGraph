package datagraph.sales;

import java.math.BigDecimal;

public record Summary(BigDecimal revenue, long orders, long units, BigDecimal averageOrderValue) {}
