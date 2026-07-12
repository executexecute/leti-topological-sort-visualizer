package ru.leti.toposort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ResultFileService {
    public void save(GraphModel graph, TopologicalSortResult result, Path path) throws IOException {
        if (result == null) {
            throw new IllegalArgumentException("Результат алгоритма отсутствует.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Не выбран путь для сохранения.");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("Визуализатор топологической сортировки — результат");
            writer.newLine();
            writer.write("Количество вершин: " + graph.getVertexCount());
            writer.newLine();
            writer.write("Количество рёбер: " + graph.getEdgeCount());
            writer.newLine();
            writer.write("Количество шагов визуализации: " + result.getSteps().size());
            writer.newLine();
            writer.newLine();

            writer.write("Начальные входящие степени:");
            writer.newLine();
            for (Map.Entry<Integer, Integer> entry : result.getInitialIndegrees().entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
            writer.newLine();

            if (result.isSuccess()) {
                writer.write("Топологическая сортировка выполнена успешно.");
                writer.newLine();
                writer.write("Топологический порядок:");
                writer.newLine();
                writer.write(result.getOrderAsText());
            } else {
                writer.write("Топологическая сортировка невозможна.");
                writer.newLine();
                writer.write("Причина: " + result.getMessage());
                writer.newLine();
                writer.write("Частично построенный порядок: " + result.getOrderAsText());
            }
            writer.newLine();
        }
    }
}
