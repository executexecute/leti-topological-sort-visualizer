package ru.leti.toposort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopologicalSortResult {
    private final boolean success;
    private final List<Integer> order;
    private final Map<Integer, Integer> initialIndegrees;
    private final String message;

    public TopologicalSortResult(boolean success, List<Integer> order, Map<Integer, Integer> initialIndegrees, String message) {
        this.success = success;
        this.order = new ArrayList<>(order);
        this.initialIndegrees = initialIndegrees;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Integer> getOrder() {
        return new ArrayList<>(order);
    }

    public Map<Integer, Integer> getInitialIndegrees() {
        return initialIndegrees;
    }

    public String getMessage() {
        return message;
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
