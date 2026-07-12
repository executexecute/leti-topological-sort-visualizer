package ru.leti.toposort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphModel {
    private final Map<Integer, Vertex> vertices = new LinkedHashMap<>();
    private final List<DirectedEdge> edges = new ArrayList<>();
    private int nextVertexId = 0;

    public Vertex addVertex(int x, int y) {
        return addVertexWithId(nextVertexId, x, y);
    }

    public Vertex addVertexWithId(int id, int x, int y) {
        if (id < 0) {
            throw new IllegalArgumentException("Идентификатор вершины не может быть отрицательным.");
        }
        if (vertices.containsKey(id)) {
            throw new IllegalArgumentException("Вершина с id " + id + " уже существует.");
        }
        Vertex vertex = new Vertex(id, x, y);
        vertices.put(vertex.getId(), vertex);
        nextVertexId = Math.max(nextVertexId, id + 1);
        return vertex;
    }

    public boolean addEdge(int from, int to) {
        if (from == to) {
            return false;
        }
        if (!vertices.containsKey(from) || !vertices.containsKey(to)) {
            return false;
        }
        if (hasEdge(from, to)) {
            return false;
        }
        edges.add(new DirectedEdge(from, to));
        return true;
    }

    public boolean removeVertex(int id) {
        Vertex removed = vertices.remove(id);
        if (removed == null) {
            return false;
        }
        edges.removeIf(edge -> edge.getFrom() == id || edge.getTo() == id);
        return true;
    }

    public boolean removeEdge(int from, int to) {
        return edges.removeIf(edge -> edge.connects(from, to));
    }

    public boolean hasEdge(int from, int to) {
        for (DirectedEdge edge : edges) {
            if (edge.connects(from, to)) {
                return true;
            }
        }
        return false;
    }

    public List<Vertex> getVertices() {
        return new ArrayList<>(vertices.values());
    }

    public List<DirectedEdge> getEdges() {
        return new ArrayList<>(edges);
    }

    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    public boolean containsVertex(int id) {
        return vertices.containsKey(id);
    }

    public int getIndegree(int vertexId) {
        int degree = 0;
        for (DirectedEdge edge : edges) {
            if (edge.getTo() == vertexId) {
                degree++;
            }
        }
        return degree;
    }

    public List<DirectedEdge> getOutgoingEdges(int vertexId) {
        List<DirectedEdge> outgoing = new ArrayList<>();
        for (DirectedEdge edge : edges) {
            if (edge.getFrom() == vertexId) {
                outgoing.add(edge);
            }
        }
        return outgoing;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public void clear() {
        vertices.clear();
        edges.clear();
        nextVertexId = 0;
    }

    public void replaceWith(GraphModel other) {
        clear();
        for (Vertex vertex : other.getVertices()) {
            addVertexWithId(vertex.getId(), vertex.getX(), vertex.getY());
        }
        for (DirectedEdge edge : other.getEdges()) {
            addEdge(edge.getFrom(), edge.getTo());
        }
    }

    public void loadDemoGraph() {
        clear();
        addVertexWithId(0, 140, 210);
        addVertexWithId(1, 390, 130);
        addVertexWithId(2, 390, 310);
        addVertexWithId(3, 680, 220);
        addVertexWithId(4, 950, 220);

        addEdge(0, 1);
        addEdge(0, 2);
        addEdge(1, 3);
        addEdge(2, 3);
        addEdge(3, 4);
    }
}
