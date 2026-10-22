package datagraph.dashboard;

import datagraph.dataset.DatasetDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** JSON data for the dashboard: the list of datasets, and each dataset's charts. */
@RestController
@RequestMapping("/api/datasets")
public class DashboardController {

    private final DashboardRepository dashboards;

    public DashboardController(DashboardRepository dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping
    public List<DatasetInfo> datasets() {
        return dashboards.definitions().stream().map(DatasetInfo::of).toList();
    }

    @GetMapping("/{id}/dashboard")
    public Dashboard dashboard(@PathVariable String id, @RequestParam(required = false) String segment) {
        DatasetDefinition definition = dashboards.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown dataset: " + id));
        if (segment != null && segment.isBlank())
            segment = null;
        if (segment != null && !definition.segments().contains(segment))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown " + definition.segmentLabel().toLowerCase() + ": " + segment);
        return dashboards.dashboard(definition, segment);
    }
}
