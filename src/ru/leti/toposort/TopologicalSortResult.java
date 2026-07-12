package ru.leti.toposort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopologicalSortResult {
    private final boolean success;
    private final List<Integer> order;
    private final Map<Integer, Integer> initialIndegrees;
    private final String message;
    private final List<AlgorithmStep> steps;

    public TopologicalSortResult(
            boolean success,
            List<Integer> order,
            Map<Integer, Integer> initialIndegrees,
            String message,
            List<AlgorithmStep> steps
    ) {
        this.success = success;
        this.order = Collections.unmodifiableList(new ArrayList<>(order));
        this.initialIndegrees = Collections.unmodifiableMap(new LinkedHashMap<>(initialIndegrees));
        this.message = message;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Integer> getOrder() {
        return order;
    }

    public Map<Integer, Integer> getInitialIndegrees() {
        return initialIndegrees;
    }

    public String getMessage() {
        return message;
    }

    public List<AlgorithmStep> getSteps() {
        return steps;
    }

    public String getOrderAsText() {
        if (order.isEmpty()) {
            return "—";
        }
        return order.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" -> "));
    }
}
