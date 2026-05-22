package domain;

import config.GameConfig;
import java.util.Locale;
import java.util.Objects;

public final class Coordinate {

    public final int x;
    public final int y;

    private Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Coordinate of(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                "Coordenada inválida: x=" + x + " y=" + y);
        }
        return new Coordinate(x, y);
    }

    public static Coordinate parse(String input, char colStart, char colEnd, int boardSize) {
        if (input == null) return null;

        String t = input.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (t.length() < 2 || t.length() > 3) return null;

        char col = t.charAt(0);
        if (col < colStart || col > colEnd) return null;

        int x = col - colStart;

        int row;
        try {
            row = Integer.parseInt(t.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }

        if (row < 1 || row > boardSize) return null;

        return new Coordinate(x, row - 1);
    }

    public static Coordinate parse(String input, GameConfig config) {
        return parse(input,
                config.getColStart(),
                config.getColEnd(),
                config.getBoardSize());
    }

    public Coordinate[] orthogonalNeighbors(int boardSize) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int count = 0;
        Coordinate[] buf = new Coordinate[4];
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                buf[count++] = new Coordinate(nx, ny);
            }
        }
        Coordinate[] result = new Coordinate[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }

    public Coordinate[] allNeighbors(int boardSize) {
        int count = 0;
        Coordinate[] buf = new Coordinate[8];
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                    buf[count++] = new Coordinate(nx, ny);
                }
            }
        }
        Coordinate[] result = new Coordinate[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }
    
    public String toLabel(char colStart) {
        return "" + (char)(colStart + x) + (y + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;
        Coordinate c = (Coordinate) o;
        return x == c.x && y == c.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Coordinate{x=" + x + ", y=" + y + "}";
    }
}
