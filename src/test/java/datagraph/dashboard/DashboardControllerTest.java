package datagraph.dashboard;

import datagraph.dashboard.Dashboard.LabeledValue;
import datagraph.dataset.DatasetDefinition;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DashboardControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DashboardRepository dashboards;

    @Autowired
    private SparkSession spark;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listsDatasetsInNavbarOrder() throws Exception {
        mvc.perform(get("/api/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("retail", "cars", "social")))
                .andExpect(jsonPath("$[1].name", is("Car Sales")))
                .andExpect(jsonPath("$[2].segmentLabel", is("Platform")))
                .andExpect(jsonPath("$[2].segments", hasSize(4)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"retail", "cars", "social"})
    void everyDatasetHasAFullDashboard(String id) throws Exception {
        mvc.perform(get("/api/datasets/{id}/dashboard", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis", hasSize(4)))
                .andExpect(jsonPath("$.kpis[0].value").value(greaterThan(0.0), Double.class))
                .andExpect(jsonPath("$.trend.labels", hasSize(24)))
                .andExpect(jsonPath("$.trend.labels[0]", is("2024-10")))
                .andExpect(jsonPath("$.bySegment.data", hasSize(4)))
                .andExpect(jsonPath("$.byGroup.data", hasSize(4)))
                .andExpect(jsonPath("$.topItems.data", hasSize(6)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"retail", "cars", "social"})
    void datasetsAreLoadedIntoSpark(String id) {
        assertThat(spark.catalog().tableExists(id)).isTrue();
        assertThat(spark.table(id).count()).isGreaterThan(1000);
    }

    @Test
    void segmentTotalsAddUpToTheOverallTotal() {
        for (DatasetDefinition d : dashboards.definitions()) {
            Dashboard all = dashboards.dashboard(d, null);
            BigDecimal sum = all.bySegment().data().stream()
                    .map(LabeledValue::value).reduce(BigDecimal.ZERO, BigDecimal::add);
            // The segment chart's metric is one of the KPIs in every dataset
            BigDecimal kpi = all.kpis().stream()
                    .filter(k -> k.label().equals(d.bySegment().metric().label()))
                    .findFirst().orElseThrow().value();
            assertThat(sum).as(d.id()).isEqualByComparingTo(kpi);
        }
    }

    @Test
    void segmentFilterNarrowsResults() {
        DatasetDefinition cars = dashboards.find("cars").orElseThrow();
        BigDecimal all = dashboards.dashboard(cars, null).kpis().get(0).value();
        BigDecimal south = dashboards.dashboard(cars, "South").kpis().get(0).value();
        assertThat(south).isPositive().isLessThan(all);
    }

    @Test
    void percentagesAreBetweenZeroAndOne() {
        for (String id : new String[]{"cars", "social"}) {
            Dashboard dashboard = dashboards.dashboard(dashboards.find(id).orElseThrow(), null);
            assertThat(dashboard.kpis().get(3).value()).as(id).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        }
    }

    @Test
    void topItemsAreSortedLargestFirst() {
        Dashboard social = dashboards.dashboard(dashboards.find("social").orElseThrow(), "Clipster");
        var top = social.topItems().data();
        for (int i = 1; i < top.size(); i++)
            assertThat(top.get(i - 1).value()).isGreaterThanOrEqualTo(top.get(i).value());
    }

    @Test
    void unknownDatasetIsNotFound() throws Exception {
        mvc.perform(get("/api/datasets/{id}/dashboard", "boats")).andExpect(status().isNotFound());
    }

    @Test
    void unknownSegmentIsRejected() throws Exception {
        mvc.perform(get("/api/datasets/cars/dashboard").param("segment", "Atlantis"))
                .andExpect(status().isBadRequest());
        // A segment from another dataset doesn't count either
        mvc.perform(get("/api/datasets/cars/dashboard").param("segment", "Clipster"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardPageAndChartLibraryAreServed() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"nav\"")));
        mvc.perform(get("/webjars/chart.js/4.5.1/dist/chart.umd.min.js"))
                .andExpect(status().isOk());
    }
}
