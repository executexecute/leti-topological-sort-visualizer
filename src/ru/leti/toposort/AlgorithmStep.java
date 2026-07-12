package ru.leti.toposort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlgorithmStep {
    private final String description;
    private final Map<Integer, Integer> indegrees;
    private final List<Integer> queue;
    private final List<Integer> result;
    private final Integer currentVertex;
    private final DirectedEdge currentEdge;
    private final boolean finished;
    private final boolean success;

    public AlgorithmStep(
            String description,
            Map<Integer, Integer> indegrees,
            List<Integer> queue,
            List<Integer> result,
            Integer currentVertex,
            DirectedEdge currentEdge,
            boolean finished,
            boolean success
    ) {
        this.description = description;
        this.indegrees = Collections.unmodifiableMap(new LinkedHashMap<>(indegrees));
        this.queue = Collections.unmodifiableList(new ArrayList<>(queue));
        this.result = Collections.unmodifiableList(new ArrayList<>(result));
        this.currentVertex = currentVertex;
        this.currentEdge = currentEdge == null
                ? null
                : new DirectedEdge(currentEdge.getFrom(), currentEdge.getTo());
        this.finished = finished;
        this.success = success;
    }

    public String getDescription() {
        return description;
    }

    public Map<Integer, Integer> getIndegrees() {
        return indegrees;
    }

    public List<Integer> getQueue() {
        return queue;
    }

    public List<Integer> getResult() {
        return result;
    }

    public Integer getCurrentVertex() {
        return currentVertex;
    }

    public DirectedEdge getCurrentEdge() {
        return currentEdge;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isSuccess() {
        return success;
    }
}
