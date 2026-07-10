package ru.leti.toposort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GraphFileService {
    public GraphModel load(Path path) throws IOException {
        var lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int index = 0;

        while (index < lines.size() && isBlankOrComment(lines.get(index))) {
            index++;
        }

        if (index >= lines.size()) {
            throw new IllegalArgumentException("Файл пуст.");
        }

        String[] verticesHeader = splitLine(lines.get(index++));
        if (verticesHeader.length != 2 || !verticesHeader[0].equalsIgnoreCase("VERTICES")) {
            throw new IllegalArgumentException("Ожидалась строка VERTICES <количество вершин>.");
        }

        int vertexCount = parseNonNegativeInt(verticesHeader[1], "Количество вершин");
        GraphModel graph = new GraphModel();

        for (int i = 0; i < vertexCount; i++) {
            index = skipBlanksAndComments(lines, index);
            if (index >= lines.size()) {
                throw new IllegalArgumentException("Недостаточно строк с вершинами.");
            }

            String[] parts = splitLine(lines.get(index++));
            if (parts.length != 3) {
                throw new IllegalArgumentException("Строка вершины должна иметь вид: <id> <x> <y>.");
            }

            int id = parseNonNegativeInt(parts[0], "id вершины");
            int x = parseInt(parts[1], "x");
            int y = parseInt(parts[2], "y");
            graph.addVertexWithId(id, x, y);
        }

        index = skipBlanksAndComments(lines, index);
        if (index >= lines.size()) {
            throw new IllegalArgumentException("Отсутствует секция EDGES.");
        }

        String[] edgesHeader = splitLine(lines.get(index++));
        if (edgesHeader.length != 2 || !edgesHeader[0].equalsIgnoreCase("EDGES")) {
            throw new IllegalArgumentException("Ожидалась строка EDGES <количество рёбер>.");
        }

        int edgeCount = parseNonNegativeInt(edgesHeader[1], "Количество рёбер");

        for (int i = 0; i < edgeCount; i++) {
            index = skipBlanksAndComments(lines, index);
            if (index >= lines.size()) {
                throw new IllegalArgumentException("Недостаточно строк с рёбрами.");
            }

            String[] parts = splitLine(lines.get(index++));
            if (parts.length != 2) {
                throw new IllegalArgumentException("Строка ребра должна иметь вид: <id начала> <id конца>.");
            }

            int from = parseNonNegativeInt(parts[0], "начало ребра");
            int to = parseNonNegativeInt(parts[1], "конец ребра");

            if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
                throw new IllegalArgumentException("Ребро " + from + " -> " + to + " ссылается на несуществующую вершину.");
            }
            if (!graph.addEdge(from, to)) {
                throw new IllegalArgumentException("Некорректное или повторяющееся ребро: " + from + " -> " + to + ".");
            }
        }

        return graph;
    }

    public void save(GraphModel graph, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("VERTICES " + graph.getVertexCount());
            writer.newLine();
            for (Vertex vertex : graph.getVertices()) {
                writer.write(vertex.getId() + " " + vertex.getX() + " " + vertex.getY());
                writer.newLine();
            }

            writer.write("EDGES " + graph.getEdgeCount());
            writer.newLine();
            for (DirectedEdge edge : graph.getEdges()) {
                writer.write(edge.getFrom() + " " + edge.getTo());
                writer.newLine();
            }
        }
    }

    private int skipBlanksAndComments(java.util.List<String> lines, int index) {
        while (index < lines.size() && isBlankOrComment(lines.get(index))) {
            index++;
        }
        return index;
    }

    private boolean isBlankOrComment(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    private String[] splitLine(String line) {
        return line.trim().split("\\s+");
    }

    private int parseNonNegativeInt(String value, String fieldName) {
        int parsed = parseInt(value, fieldName);
        if (parsed < 0) {
            throw new IllegalArgumentException(fieldName + " не может быть отрицательным.");
        }
        return parsed;
    }

    private int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректное число в поле " + fieldName + ": " + value + ".");
        }
    }
}
