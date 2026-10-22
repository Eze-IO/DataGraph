package datagraph.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SalesControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SalesRepository sales;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void summaryCoversAllSampleOrders() throws Exception {
        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", greaterThan(2000)))
                .andExpect(jsonPath("$.revenue", greaterThan(0.0)));
    }

    @Test
    void monthlyRevenueHasOnePointPerMonth() throws Exception {
        mvc.perform(get("/api/revenue/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[0].label", is("2024-10")))
                .andExpect(jsonPath("$[23].label", is("2026-09")));
    }

    @Test
    void regionTotalsAddUpToOverallRevenue() {
        BigDecimal total = sales.summary(null).revenue();
        BigDecimal byRegion = sales.revenueByRegion().stream()
                .map(LabeledValue::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(byRegion).isEqualByComparingTo(total);
    }

    @Test
    void regionFilterNarrowsResults() {
        long all = sales.summary(null).orders();
        long north = sales.summary("North").orders();
        assertThat(north).isPositive().isLessThan(all);
    }

    @Test
    void topProductsAreSortedAndLimited() {
        List<LabeledValue> top = sales.topProducts(null, 3);
        assertThat(top).hasSize(3);
        assertThat(top.get(0).value()).isGreaterThanOrEqualTo(top.get(1).value());
    }

    @Test
    void everyEndpointWorksWithARegionFilter() throws Exception {
        for (String path : List.of("/api/summary", "/api/revenue/monthly", "/api/revenue/by-category", "/api/top-products"))
            mvc.perform(get(path).param("region", "West")).andExpect(status().isOk());
        mvc.perform(get("/api/revenue/by-category").param("region", "West"))
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void unknownRegionIsRejected() throws Exception {
        mvc.perform(get("/api/summary").param("region", "Atlantis"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardPageIsServed() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("chart-monthly")));
    }

    @Test
    void chartLibraryIsServedLocally() throws Exception {
        mvc.perform(get("/webjars/chart.js/4.5.1/dist/chart.umd.min.js"))
                .andExpect(status().isOk());
    }
}
