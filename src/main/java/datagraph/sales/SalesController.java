package datagraph.sales;

import datagraph.sample.SampleDataGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** JSON data for the dashboard charts. */
@RestController
@RequestMapping("/api")
public class SalesController {

    private final SalesRepository sales;

    public SalesController(SalesRepository sales) {
        this.sales = sales;
    }

    @GetMapping("/regions")
    public List<String> regions() {
        return SampleDataGenerator.REGIONS;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) String region) {
        return sales.summary(checkRegion(region));
    }

    @GetMapping("/revenue/monthly")
    public List<LabeledValue> revenueByMonth(@RequestParam(required = false) String region) {
        return sales.revenueByMonth(checkRegion(region));
    }

    //Always covers every region so the selected one can be compared with the rest
    @GetMapping("/revenue/by-region")
    public List<LabeledValue> revenueByRegion() {
        return sales.revenueByRegion();
    }

    @GetMapping("/revenue/by-category")
    public List<LabeledValue> revenueByCategory(@RequestParam(required = false) String region) {
        return sales.revenueByCategory(checkRegion(region));
    }

    @GetMapping("/top-products")
    public List<LabeledValue> topProducts(@RequestParam(required = false) String region,
                                          @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
        return sales.topProducts(checkRegion(region), limit);
    }

    private static String checkRegion(String region) {
        if (region == null || region.isBlank())
            return null;
        if (!SampleDataGenerator.REGIONS.contains(region))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown region: " + region);
        return region;
    }
}
