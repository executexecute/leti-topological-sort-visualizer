package ru.leti.toposort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GraphFileService {
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_VERTICES = 2_000;
    private static final int MAX_EDGES = 10_000;
    private static final int MAX_COORDINATE = 100_000;

    public GraphModel load(Path path) throws IOException {
        validateFile(path);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int index = skipBlanksAndComments(lines, 0);

        if (index >= lines.size()) {
            throw new IllegalArgumentException("Файл пуст или содержит только комментарии.");
        }

        String[] verticesHeader = splitLine(lines.get(index));
        if (verticesHeader.length != 2 || !verticesHeader[0].equalsIgnoreCase("VERTICES")) {
            throw formatError(index, "ожидалась строка VERTICES <количество вершин>");
        }
        index++;

        int vertexCount = parseNonNegativeInt(verticesHeader[1], "количество вершин", index);
        if (vertexCount > MAX_VERTICES) {
            throw new IllegalArgumentException("Слишком много вершин: максимум " + MAX_VERTICES + ".");
        }

        GraphModel graph = new GraphModel();
        for (int i = 0; i < vertexCount; i++) {
            index = skipBlanksAndComments(lines, index);
            if (index >= lines.size()) {
                throw new IllegalArgumentException("Недостаточно строк с вершинами: ожидалось " + vertexCount + ".");
            }

            String[] parts = splitLine(lines.get(index));
            if (parts.length != 3) {
                throw formatError(index, "вершина должна иметь вид <id> <x> <y>");
            }

            int id = parseNonNegativeInt(parts[0], "id вершины", index + 1);
            int x = parseCoordinate(parts[1], "x", index + 1);
            int y = parseCoordinate(parts[2], "y", index + 1);
            try {
                graph.addVertexWithId(id, x, y);
            } catch (IllegalArgumentException exception) {
                throw formatError(index, exception.getMessage());
            }
            index++;
        }

        index = skipBlanksAndComments(lines, index);
        if (index >= lines.size()) {
            throw new IllegalArgumentException("Отсутствует секция EDGES.");
        }

        String[] edgesHeader = splitLine(lines.get(index));
        if (edgesHeader.length != 2 || !edgesHeader[0].equalsIgnoreCase("EDGES")) {
            throw formatError(index, "ожидалась строка EDGES <количество рёбер>");
        }
        index++;

        int edgeCount = parseNonNegativeInt(edgesHeader[1], "количество рёбер", index);
        if (edgeCount > MAX_EDGES) {
            throw new IllegalArgumentException("Слишком много рёбер: максимум " + MAX_EDGES + ".");
        }

        for (int i = 0; i < edgeCount; i++) {
            index = skipBlanksAndComments(lines, index);
            if (index >= lines.size()) {
                throw new IllegalArgumentException("Недостаточно строк с рёбрами: ожидалось " + edgeCount + ".");
            }

            String[] parts = splitLine(lines.get(index));
            if (parts.length != 2) {
                throw formatError(index, "ребро должно иметь вид <id начала> <id конца>");
            }

            int from = parseNonNegativeInt(parts[0], "начало ребра", index + 1);
            int to = parseNonNegativeInt(parts[1], "конец ребра", index + 1);

            if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
                throw formatError(index, "ребро " + from + " -> " + to + " ссылается на несуществующую вершину");
            }
            if (!graph.addEdge(from, to)) {
                throw formatError(index, "петля или повторяющееся ребро: " + from + " -> " + to);
            }
            index++;
        }

        index = skipBlanksAndComments(lines, index);
        if (index < lines.size()) {
            throw formatError(index, "после заявленного списка рёбер обнаружены лишние данные");
        }

        return graph;
    }

    public void save(GraphModel graph, Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Не выбран путь для сохранения.");
        }
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

    private void validateFile(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Файл не выбран.");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Файл не существует.");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Выбранный объект не является обычным файлом.");
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Нет доступа на чтение файла.");
        }
        if (Files.size(path) > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Файл слишком большой: максимум 2 МБ.");
        }
    }

    private int skipBlanksAndComments(List<String> lines, int index) {
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

    private int parseNonNegativeInt(String value, String fieldName, int lineNumber) {
        int parsed = parseInt(value, fieldName, lineNumber);
        if (parsed < 0) {
            throw new IllegalArgumentException("Строка " + lineNumber + ": " + fieldName + " не может быть отрицательным.");
        }
        return parsed;
    }

    private int parseCoordinate(String value, String fieldName, int lineNumber) {
        int parsed = parseInt(value, fieldName, lineNumber);
        if (parsed < 0 || parsed > MAX_COORDINATE) {
            throw new IllegalArgumentException(
                    "Строка " + lineNumber + ": координата " + fieldName
                            + " должна быть в диапазоне от 0 до " + MAX_COORDINATE + "."
            );
        }
        return parsed;
    }

    private int parseInt(String value, String fieldName, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Строка " + lineNumber + ": поле «" + fieldName + "» не является целым числом: " + value + "."
            );
        }
    }

    private IllegalArgumentException formatError(int zeroBasedLine, String message) {
        return new IllegalArgumentException("Строка " + (zeroBasedLine + 1) + ": " + message + ".");
    }
}
