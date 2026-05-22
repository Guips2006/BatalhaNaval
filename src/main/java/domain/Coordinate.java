// =========================================================
// domain/Coordinate.java
// =========================================================
package domain;

import config.GameConfig;
import java.util.Locale;
import java.util.Objects;

/**
 * Par (x, y) imutável que representa uma célula do tabuleiro.
 *
 * x = coluna (0-based, A → 0)
 * y = linha  (0-based, 1 → 0)
 *
 * Toda conversão de string do usuário passa por aqui.
 * Nenhum System.out — a classe é puro domínio.
 */
public final class Coordinate {

    public final int x;
    public final int y;

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------

    private Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constrói a partir de índices já validados externamente.
     * Lança {@link IllegalArgumentException} se os índices forem negativos.
     */
    public static Coordinate of(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                "Coordenada inválida: x=" + x + " y=" + y);
        }
        return new Coordinate(x, y);
    }

    /**
     * Faz o parse de uma string como "B7" ou "J10".
     *
     * @param input     Texto digitado pelo usuário (case-insensitive, espaços ignorados)
     * @param colStart  Primeira letra de coluna válida (ex. 'A')
     * @param colEnd    Última letra de coluna válida  (ex. 'J')
     * @param boardSize Tamanho do tabuleiro (linhas 1..boardSize)
     * @return Coordinate parseada, ou {@code null} se o formato for inválido
     */
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

    /** Sobrecarga que lê limites direto do GameConfig. */
    public static Coordinate parse(String input, GameConfig config) {
        return parse(input,
                config.getColStart(),
                config.getColEnd(),
                config.getBoardSize());
    }

    // ------------------------------------------------------------------
    // Vizinhança
    // ------------------------------------------------------------------

    /**
     * Retorna os vizinhos ortogonais (cima/baixo/esquerda/direita) que
     * existem dentro do tabuleiro de tamanho {@code boardSize}.
     */
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

    /**
     * Retorna todos os vizinhos (8 direções) dentro do tabuleiro.
     */
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

    // ------------------------------------------------------------------
    // Formatação
    // ------------------------------------------------------------------

    /**
     * Converte de volta para string legível ("B7").
     */
    public String toLabel(char colStart) {
        return "" + (char)(colStart + x) + (y + 1);
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

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