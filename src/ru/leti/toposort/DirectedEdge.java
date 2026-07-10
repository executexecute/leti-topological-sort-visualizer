package ru.leti.toposort;

public class DirectedEdge {
    private final int from;
    private final int to;

    public DirectedEdge(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public boolean connects(int from, int to) {
        return this.from == from && this.to == to;
    }
}
