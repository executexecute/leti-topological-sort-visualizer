package ru.leti.toposort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class TopologicalSorter {
    public TopologicalSortResult sort(GraphModel graph) {
        if (graph.getVertexCount() == 0) {
            return new TopologicalSortResult(false, List.of(), Map.of(), "Граф пуст. Топологическая сортировка не запущена.");
        }

        Map<Integer, Integer> indegrees = new LinkedHashMap<>();
        for (Vertex vertex : graph.getVertices()) {
            indegrees.put(vertex.getId(), 0);
        }

        for (DirectedEdge edge : graph.getEdges()) {
            int to = edge.getTo();
            indegrees.put(to, indegrees.get(to) + 1);
        }

        Map<Integer, Integer> initialIndegrees = new LinkedHashMap<>(indegrees);
        Queue<Integer> queue = new ArrayDeque<>();

        for (Map.Entry<Integer, Integer> entry : indegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Integer> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            int current = queue.remove();
            result.add(current);

            for (DirectedEdge edge : graph.getOutgoingEdges(current)) {
                int neighbor = edge.getTo();
                int newDegree = indegrees.get(neighbor) - 1;
                indegrees.put(neighbor, newDegree);

                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (result.size() != graph.getVertexCount()) {
            return new TopologicalSortResult(
                    false,
                    result,
                    initialIndegrees,
                    "Топологическая сортировка невозможна: в графе обнаружен цикл."
            );
        }

        return new TopologicalSortResult(
                true,
                result,
                initialIndegrees,
                "Топологическая сортировка выполнена успешно."
        );
    }
}
