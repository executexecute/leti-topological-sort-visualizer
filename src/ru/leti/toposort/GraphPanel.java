package ru.leti.toposort;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphPanel extends JPanel {
    private static final int RADIUS = 27;
    private static final int EDGE_SELECTION_DISTANCE = 12;
    private static final int CANVAS_MARGIN = 180;

    private final GraphModel graph;
    private final Runnable onGraphChanged;
    private final MessageConsumer messageConsumer;

    private Mode mode = Mode.ADD_VERTEX;
    private Integer selectedEdgeStart = null;
    private Vertex draggedVertex = null;
    private Point previousDragPoint = null;

    private Integer highlightedVertex = null;
    private DirectedEdge highlightedEdge = null;
    private Set<Integer> processedVertices = Collections.emptySet();
    private Set<Integer> queuedVertices = Collections.emptySet();
    private Map<Integer, Integer> displayedIndegrees = Collections.emptyMap();

    public GraphPanel(GraphModel graph, Runnable onGraphChanged, MessageConsumer messageConsumer) {
        this.graph = graph;
        this.onGraphChanged = onGraphChanged;
        this.messageConsumer = messageConsumer;
        setBackground(new Color(250, 250, 250));
        updateCanvasSize();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                draggedVertex = null;
                previousDragPoint = null;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        selectedEdgeStart = null;
        draggedVertex = null;
        previousDragPoint = null;
        repaint();
    }

    public void clearAlgorithmState() {
        highlightedVertex = null;
        highlightedEdge = null;
        processedVertices = Collections.emptySet();
        queuedVertices = Collections.emptySet();
        displayedIndegrees = Collections.emptyMap();
        repaint();
    }

    public void setAlgorithmState(AlgorithmStep step) {
        if (step == null) {
            clearAlgorithmState();
            return;
        }
        highlightedVertex = step.getCurrentVertex();
        highlightedEdge = step.getCurrentEdge();
        processedVertices = new HashSet<>(step.getResult());
        queuedVertices = new HashSet<>(step.getQueue());
        displayedIndegrees = step.getIndegrees();
        repaint();
    }

    public void updateCanvasSize() {
        int maxX = 1000;
        int maxY = 650;
        for (Vertex vertex : graph.getVertices()) {
            maxX = Math.max(maxX, vertex.getX() + CANVAS_MARGIN);
            maxY = Math.max(maxY, vertex.getY() + CANVAS_MARGIN);
        }
        setPreferredSize(new Dimension(maxX, maxY));
        revalidate();
    }

    private void handleMousePressed(MouseEvent event) {
        switch (mode) {
            case ADD_VERTEX -> addVertex(event);
            case ADD_EDGE -> addEdge(event);
            case MOVE_VERTEX -> beginMoveVertex(event);
            case DELETE_VERTEX -> deleteVertex(event);
            case DELETE_EDGE -> deleteEdge(event);
        }
    }

    private void addVertex(MouseEvent event) {
        Vertex created = graph.addVertex(Math.max(RADIUS, event.getX()), Math.max(RADIUS, event.getY()));
        messageConsumer.accept("Добавлена вершина " + created.getId() + ".");
        graphChanged();
    }

    private void addEdge(MouseEvent event) {
        Vertex clicked = findVertex(event.getX(), event.getY());
        if (clicked == null) {
            selectedEdgeStart = null;
            messageConsumer.accept("Для создания ребра сначала выберите вершину, затем вторую вершину.");
            repaint();
            return;
        }

        if (selectedEdgeStart == null) {
            selectedEdgeStart = clicked.getId();
            messageConsumer.accept("Начало ребра: " + selectedEdgeStart + ". Теперь выберите конечную вершину.");
            repaint();
            return;
        }

        int from = selectedEdgeStart;
        int to = clicked.getId();
        selectedEdgeStart = null;
        if (graph.addEdge(from, to)) {
            messageConsumer.accept("Добавлено ребро " + from + " -> " + to + ".");
            graphChanged();
        } else {
            messageConsumer.accept("Ребро не добавлено: запрещены петли, повторы и ссылки на несуществующие вершины.");
            repaint();
        }
    }

    private void beginMoveVertex(MouseEvent event) {
        draggedVertex = findVertex(event.getX(), event.getY());
        previousDragPoint = draggedVertex == null ? null : event.getPoint();
        if (draggedVertex == null) {
            messageConsumer.accept("Вершина не выбрана. Нажмите по кругу вершины.");
        } else {
            messageConsumer.accept("Перемещается вершина " + draggedVertex.getId() + ".");
        }
        repaint();
    }

    private void deleteVertex(MouseEvent event) {
        Vertex vertex = findVertex(event.getX(), event.getY());
        if (vertex == null) {
            messageConsumer.accept("Вершина не выбрана. Нажмите по кругу вершины.");
            return;
        }
        int id = vertex.getId();
        graph.removeVertex(id);
        messageConsumer.accept("Удалена вершина " + id + " и все связанные с ней рёбра.");
        graphChanged();
    }

    private void deleteEdge(MouseEvent event) {
        DirectedEdge edge = findEdge(event.getX(), event.getY());
        if (edge == null) {
            messageConsumer.accept("Ребро не выбрано. Нажмите ближе к линии ребра.");
            return;
        }
        graph.removeEdge(edge.getFrom(), edge.getTo());
        messageConsumer.accept("Удалено ребро " + edge.getFrom() + " -> " + edge.getTo() + ".");
        graphChanged();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (mode != Mode.MOVE_VERTEX || draggedVertex == null || previousDragPoint == null) {
            return;
        }

        int dx = event.getX() - previousDragPoint.x;
        int dy = event.getY() - previousDragPoint.y;
        draggedVertex.setPosition(
                Math.max(RADIUS, draggedVertex.getX() + dx),
                Math.max(RADIUS, draggedVertex.getY() + dy)
        );
        previousDragPoint = event.getPoint();
        updateCanvasSize();
        onGraphChanged.run();
        repaint();
    }

    private void graphChanged() {
        updateCanvasSize();
        onGraphChanged.run();
        repaint();
    }

    private Vertex findVertex(int x, int y) {
        for (Vertex vertex : graph.getVertices()) {
            int dx = vertex.getX() - x;
            int dy = vertex.getY() - y;
            if (dx * dx + dy * dy <= RADIUS * RADIUS) {
                return vertex;
            }
        }
        return null;
    }

    private DirectedEdge findEdge(int x, int y) {
        DirectedEdge closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (DirectedEdge edge : graph.getEdges()) {
            Vertex from = graph.getVertex(edge.getFrom());
            Vertex to = graph.getVertex(edge.getTo());
            if (from == null || to == null) {
                continue;
            }
            double distance = distanceToSegment(x, y, from.getX(), from.getY(), to.getX(), to.getY());
            if (distance <= EDGE_SELECTION_DISTANCE && distance < closestDistance) {
                closest = edge;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projectionX = x1 + t * dx;
        double projectionY = y1 + t * dy;
        return Math.hypot(px - projectionX, py - projectionY);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawGrid(g);

        for (DirectedEdge edge : graph.getEdges()) {
            drawEdge(g, edge);
        }
        for (Vertex vertex : graph.getVertices()) {
            drawVertex(g, vertex);
        }

        if (selectedEdgeStart != null) {
            Vertex vertex = graph.getVertex(selectedEdgeStart);
            if (vertex != null) {
                g.setColor(new Color(30, 90, 180));
                g.drawString("Начало ребра: " + selectedEdgeStart, vertex.getX() - 50, vertex.getY() - 38);
            }
        }
        g.dispose();
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(235, 235, 235));
        for (int x = 0; x < getWidth(); x += 50) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 50) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawEdge(Graphics2D g, DirectedEdge edge) {
        Vertex from = graph.getVertex(edge.getFrom());
        Vertex to = graph.getVertex(edge.getTo());
        if (from == null || to == null) {
            return;
        }

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) {
            return;
        }

        double unitX = dx / length;
        double unitY = dy / length;
        int startX = (int) Math.round(from.getX() + unitX * RADIUS);
        int startY = (int) Math.round(from.getY() + unitY * RADIUS);
        int endX = (int) Math.round(to.getX() - unitX * RADIUS);
        int endY = (int) Math.round(to.getY() - unitY * RADIUS);

        boolean highlighted = highlightedEdge != null && highlightedEdge.connects(edge.getFrom(), edge.getTo());
        if (highlighted) {
            g.setStroke(new BasicStroke(5f));
            g.setColor(new Color(220, 80, 30));
        } else {
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(70, 70, 70));
        }

        g.drawLine(startX, startY, endX, endY);
        drawArrowHead(g, startX, startY, endX, endY);

        int labelX = (startX + endX) / 2;
        int labelY = (startY + endY) / 2;
        g.setColor(new Color(55, 55, 55));
        g.drawString(edge.getFrom() + " -> " + edge.getTo(), labelX + 6, labelY - 6);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawArrowHead(Graphics2D g, int startX, int startY, int endX, int endY) {
        double angle = Math.atan2(endY - startY, endX - startX);
        int arrowLength = 14;
        double arrowAngle = Math.PI / 7;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(endX, endY);
        arrow.lineTo(endX - arrowLength * Math.cos(angle - arrowAngle), endY - arrowLength * Math.sin(angle - arrowAngle));
        arrow.lineTo(endX - arrowLength * Math.cos(angle + arrowAngle), endY - arrowLength * Math.sin(angle + arrowAngle));
        arrow.closePath();
        g.fill(arrow);
    }

    private void drawVertex(Graphics2D g, Vertex vertex) {
        int x = vertex.getX();
        int y = vertex.getY();
        boolean current = highlightedVertex != null && highlightedVertex == vertex.getId();
        boolean processed = processedVertices.contains(vertex.getId());
        boolean queued = queuedVertices.contains(vertex.getId());
        boolean selected = selectedEdgeStart != null && selectedEdgeStart == vertex.getId();
        boolean beingMoved = draggedVertex != null && draggedVertex.getId() == vertex.getId();

        if (current) {
            g.setColor(new Color(255, 220, 150));
        } else if (processed) {
            g.setColor(new Color(205, 240, 210));
        } else if (queued) {
            g.setColor(new Color(205, 225, 255));
        } else if (beingMoved) {
            g.setColor(new Color(220, 235, 255));
        } else if (selected) {
            g.setColor(new Color(200, 225, 255));
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        g.setStroke(new BasicStroke(current || selected || beingMoved ? 4f : 2f));
        if (current) {
            g.setColor(new Color(220, 120, 0));
        } else if (processed) {
            g.setColor(new Color(45, 135, 65));
        } else if (queued) {
            g.setColor(new Color(55, 105, 190));
        } else if (beingMoved) {
            g.setColor(new Color(45, 110, 200));
        } else {
            g.setColor(new Color(40, 40, 40));
        }
        g.drawOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        String idText = String.valueOf(vertex.getId());
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.BLACK);
        g.drawString(idText, x - metrics.stringWidth(idText) / 2, y + metrics.getAscent() / 2 - 3);

        int degree = displayedIndegrees.containsKey(vertex.getId())
                ? displayedIndegrees.get(vertex.getId())
                : graph.getIndegree(vertex.getId());
        g.setColor(new Color(80, 80, 80));
        g.drawString("deg=" + degree, x - RADIUS, y + RADIUS + 17);
        g.setStroke(new BasicStroke(1f));
    }

    public interface MessageConsumer {
        void accept(String message);
    }
}
