package ru.leti.toposort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ResultFileService {
    public void save(GraphModel graph, TopologicalSortResult result, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            if (result.isSuccess()) {
                writer.write("Топологическая сортировка выполнена успешно.");
                writer.newLine();
                writer.newLine();
                writer.write("Количество вершин: " + graph.getVertexCount());
                writer.newLine();
                writer.write("Количество рёбер: " + graph.getEdgeCount());
                writer.newLine();
                writer.newLine();
                writer.write("Начальные входящие степени:");
                writer.newLine();
                for (Map.Entry<Integer, Integer> entry : result.getInitialIndegrees().entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue());
                    writer.newLine();
                }
                writer.newLine();
                writer.write("Топологический порядок:");
                writer.newLine();
                writer.write(result.getOrderAsText());
                writer.newLine();
            } else {
                writer.write("Топологическая сортировка невозможна.");
                writer.newLine();
                writer.write("Причина: " + result.getMessage());
                writer.newLine();
                writer.write("Частично построенный порядок: " + result.getOrderAsText());
                writer.newLine();
            }
        }
    }
}
