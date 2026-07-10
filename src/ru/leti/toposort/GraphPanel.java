package ru.leti.toposort;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

public class GraphPanel extends JPanel {
    private static final int RADIUS = 26;

    private final GraphModel graph;
    private Mode mode = Mode.MOVE;
    private Integer selectedEdgeStart = null;
    private Vertex draggedVertex = null;
    private Integer highlightedVertex = null;
    private final Runnable onGraphChanged;
    private final MessageConsumer messageConsumer;

    public GraphPanel(GraphModel graph, Runnable onGraphChanged, MessageConsumer messageConsumer) {
        this.graph = graph;
        this.onGraphChanged = onGraphChanged;
        this.messageConsumer = messageConsumer;

        setPreferredSize(new Dimension(1300, 850));
        setBackground(new Color(250, 250, 250));

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
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        this.selectedEdgeStart = null;
        repaint();
    }

    public void setHighlightedVertex(Integer highlightedVertex) {
        this.highlightedVertex = highlightedVertex;
        repaint();
    }

    private void handleMousePressed(MouseEvent event) {
        if (mode == Mode.ADD_VERTEX) {
            Vertex created = graph.addVertex(event.getX(), event.getY());
            messageConsumer.accept("Добавлена вершина " + created.getId() + ".");
            onGraphChanged.run();
            repaint();
            return;
        }

        Vertex clicked = findVertex(event.getX(), event.getY());

        if (mode == Mode.ADD_EDGE) {
            if (clicked == null) {
                selectedEdgeStart = null;
                messageConsumer.accept("Для создания ребра выберите две вершины.");
                repaint();
                return;
            }

            if (selectedEdgeStart == null) {
                selectedEdgeStart = clicked.getId();
                messageConsumer.accept("Выбрана начальная вершина " + selectedEdgeStart + ". Теперь выберите конечную вершину.");
            } else {
                boolean added = graph.addEdge(selectedEdgeStart, clicked.getId());
                if (added) {
                    messageConsumer.accept("Добавлено ребро " + selectedEdgeStart + " -> " + clicked.getId() + ".");
                } else {
                    messageConsumer.accept("Ребро не добавлено: петли и повторяющиеся рёбра не поддерживаются.");
                }
                selectedEdgeStart = null;
                onGraphChanged.run();
            }
            repaint();
            return;
        }

        if (mode == Mode.MOVE) {
            draggedVertex = clicked;
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (mode == Mode.MOVE && draggedVertex != null) {
            draggedVertex.setPosition(event.getX(), event.getY());
            onGraphChanged.run();
            repaint();
        }
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
                g.drawString("Начало ребра: " + selectedEdgeStart, vertex.getX() - 45, vertex.getY() - 36);
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

        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(70, 70, 70));
        g.drawLine(startX, startY, endX, endY);
        drawArrowHead(g, startX, startY, endX, endY);

        int labelX = (startX + endX) / 2;
        int labelY = (startY + endY) / 2;
        g.setColor(new Color(50, 50, 50));
        g.drawString(edge.getFrom() + " -> " + edge.getTo(), labelX + 6, labelY - 6);

        g.setStroke(new BasicStroke(1f));
    }

    private void drawArrowHead(Graphics2D g, int startX, int startY, int endX, int endY) {
        double angle = Math.atan2(endY - startY, endX - startX);
        int arrowLength = 14;
        double arrowAngle = Math.PI / 7;

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(endX, endY);
        arrow.lineTo(
                endX - arrowLength * Math.cos(angle - arrowAngle),
                endY - arrowLength * Math.sin(angle - arrowAngle)
        );
        arrow.lineTo(
                endX - arrowLength * Math.cos(angle + arrowAngle),
                endY - arrowLength * Math.sin(angle + arrowAngle)
        );
        arrow.closePath();
        g.fill(arrow);
    }

    private void drawVertex(Graphics2D g, Vertex vertex) {
        int x = vertex.getX();
        int y = vertex.getY();

        boolean highlighted = highlightedVertex != null && highlightedVertex == vertex.getId();
        boolean selected = selectedEdgeStart != null && selectedEdgeStart == vertex.getId();

        if (highlighted) {
            g.setColor(new Color(210, 240, 210));
        } else if (selected) {
            g.setColor(new Color(200, 225, 255));
        } else {
            g.setColor(Color.WHITE);
        }

        g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        g.setStroke(new BasicStroke(highlighted || selected ? 4f : 2f));
        g.setColor(highlighted ? new Color(50, 140, 70) : new Color(40, 40, 40));
        g.drawOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        String idText = String.valueOf(vertex.getId());
        FontMetrics metrics = g.getFontMetrics();
        int textX = x - metrics.stringWidth(idText) / 2;
        int textY = y + metrics.getAscent() / 2 - 3;
        g.setColor(Color.BLACK);
        g.drawString(idText, textX, textY);

        String degreeText = "deg=" + graph.getIndegree(vertex.getId());
        g.setColor(new Color(80, 80, 80));
        g.drawString(degreeText, x - RADIUS, y + RADIUS + 17);

        g.setStroke(new BasicStroke(1f));
    }

    public interface MessageConsumer {
        void accept(String message);
    }
}
