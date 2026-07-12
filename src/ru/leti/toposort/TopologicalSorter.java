package ru.leti.toposort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class TopologicalSorter {
    public TopologicalSortResult sort(GraphModel graph) {
        List<AlgorithmStep> steps = new ArrayList<>();

        if (graph.getVertexCount() == 0) {
            String message = "Граф пуст. Топологическая сортировка не запущена.";
            steps.add(new AlgorithmStep(
                    message,
                    Map.of(),
                    List.of(),
                    List.of(),
                    null,
                    null,
                    true,
                    false
            ));
            return new TopologicalSortResult(false, List.of(), Map.of(), message, steps);
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
        steps.add(snapshot(
                "Входящие степени рассчитаны. В очередь добавлены все вершины с входящей степенью 0.",
                indegrees,
                queue,
                result,
                null,
                null,
                false,
                false
        ));

        while (!queue.isEmpty()) {
            int current = queue.remove();
            result.add(current);

            steps.add(snapshot(
                    "Вершина " + current + " извлечена из очереди и добавлена в текущий результат.",
                    indegrees,
                    queue,
                    result,
                    current,
                    null,
                    false,
                    false
            ));

            for (DirectedEdge edge : graph.getOutgoingEdges(current)) {
                int neighbor = edge.getTo();
                int oldDegree = indegrees.get(neighbor);
                int newDegree = oldDegree - 1;
                indegrees.put(neighbor, newDegree);

                StringBuilder description = new StringBuilder();
                description.append("Обработано ребро ")
                        .append(edge.getFrom())
                        .append(" -> ")
                        .append(edge.getTo())
                        .append(". Входящая степень вершины ")
                        .append(neighbor)
                        .append(" уменьшена: ")
                        .append(oldDegree)
                        .append(" -> ")
                        .append(newDegree)
                        .append(".");

                if (newDegree == 0) {
                    queue.add(neighbor);
                    description.append(" Вершина ")
                            .append(neighbor)
                            .append(" добавлена в очередь, потому что её входящая степень стала 0.");
                }

                steps.add(snapshot(
                        description.toString(),
                        indegrees,
                        queue,
                        result,
                        current,
                        edge,
                        false,
                        false
                ));
            }
        }

        boolean success = result.size() == graph.getVertexCount();
        String message;
        if (success) {
            message = "Топологическая сортировка выполнена успешно. Итоговый порядок: "
                    + joinOrder(result) + ".";
        } else {
            message = "Топологическая сортировка невозможна: в графе обнаружен цикл. "
                    + "В результат попали не все вершины.";
        }

        steps.add(snapshot(
                message,
                indegrees,
                queue,
                result,
                null,
                null,
                true,
                success
        ));

        return new TopologicalSortResult(
                success,
                result,
                initialIndegrees,
                success
                        ? "Топологическая сортировка выполнена успешно."
                        : "Топологическая сортировка невозможна: в графе обнаружен цикл.",
                steps
        );
    }

    private AlgorithmStep snapshot(
            String description,
            Map<Integer, Integer> indegrees,
            Queue<Integer> queue,
            List<Integer> result,
            Integer currentVertex,
            DirectedEdge currentEdge,
            boolean finished,
            boolean success
    ) {
        return new AlgorithmStep(
                description,
                indegrees,
                new ArrayList<>(queue),
                result,
                currentVertex,
                currentEdge,
                finished,
                success
        );
    }

    private String joinOrder(List<Integer> order) {
        if (order.isEmpty()) {
            return "—";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(order.get(i));
        }
        return builder.toString();
    }
}
