// =========================================================
// ui/TerminalUI.java
// =========================================================
package ui;

import config.GameConfig;
import domain.Board;
import java.util.List;

/**
 * Toda a saída visual do jogo no terminal.
 * Nenhuma lógica de regra aqui — só formatação e impressão.
 */
public class TerminalUI {

    private final GameConfig config;
    private final char       colStart;
    private final int        boardSize;

    public TerminalUI(GameConfig config) {
        this.config    = config;
        this.colStart  = config.getColStart();
        this.boardSize = config.getBoardSize();
    }

    // ------------------------------------------------------------------
    // Tabuleiros
    // ------------------------------------------------------------------

    /**
     * Dois tabuleiros lado a lado: próprio (com navios) e tiros disparados.
     *
     * @param own       Board do jogador (getShipCell para o lado esquerdo)
     * @param shotsView Board de tiros disparados (getShotCell para o lado direito)
     */
    public void printTwoBoards(Board own, Board shotsView) {
        String leftTitle  = "SEU TABULEIRO";
        String rightTitle = "TIROS NO INIMIGO";

        System.out.printf("%-30s | %s%n", leftTitle, rightTitle);
        String header = buildColumnHeader();
        System.out.println(header + "     |     " + header);

        for (int y = 0; y < boardSize; y++) {
            StringBuilder left  = new StringBuilder(String.format("%2d  ", y + 1));
            StringBuilder right = new StringBuilder(String.format("%2d  ", y + 1));

            for (int x = 0; x < boardSize; x++) {
                char shipCell = own.getShipCell(x, y);
                // Ocultar navios intactos se configurado
                if (!config.isShowOwnShips() && shipCell == Board.SHIP) shipCell = Board.EMPTY;
                left.append(shipCell).append(' ');
                right.append(shotsView.getShotCell(x, y)).append(' ');
            }

            System.out.println(left + "  |  " + right);
        }

        if (config.isShowLegend()) {
            System.out.println("Legenda: S navio  X acerto  o água  . desconhecido");
        }
    }

    /**
     * Um único tabuleiro com título.
     *
     * @param title      Título impresso acima
     * @param board      Board a exibir
     * @param showShips  Se false, células 'S' aparecem como '.'
     */
    public void printSingleBoard(String title, Board board, boolean showShips) {
        System.out.println(title);
        System.out.println(buildColumnHeader());
        for (int y = 0; y < boardSize; y++) {
            StringBuilder row = new StringBuilder(String.format("%2d  ", y + 1));
            for (int x = 0; x < boardSize; x++) {
                char c = board.getShipCell(x, y);
                if (!showShips && c == Board.SHIP) c = Board.EMPTY;
                row.append(c).append(' ');
            }
            System.out.println(row);
        }
    }

    // ------------------------------------------------------------------
    // Log
    // ------------------------------------------------------------------

    /** Imprime as últimas {@code n} entradas do log. */
    public void printLogTail(List<String> log, int n) {
        System.out.println("Últimos eventos:");
        int start = Math.max(0, log.size() - n);
        for (int i = start; i < log.size(); i++) {
            System.out.println((i + 1) + ") " + log.get(i));
        }
    }

    /** Imprime o log completo. */
    public void printFullLog(List<String> log) {
        System.out.println("=== LOG COMPLETO ===");
        for (int i = 0; i < log.size(); i++) {
            System.out.println((i + 1) + ") " + log.get(i));
        }
    }

    // ------------------------------------------------------------------
    // Mensagens de partida
    // ------------------------------------------------------------------

    public void printWelcome(String version) {
        System.out.println("=== BATALHA NAVAL v" + version + " ===");
    }

    public void printTurnHeader(String playerName, int ownShips, int enemyShips) {
        System.out.println();
        System.out.printf("Navios restantes — %s: %d | Inimigo: %d%n",
                playerName, ownShips, enemyShips);
    }

    public void printVictory(String winnerName) {
        System.out.println();
        System.out.println("=== FIM DE JOGO ===");
        System.out.println("VITÓRIA: " + winnerName + " afundou toda a frota inimiga!");
    }

    public void printDefeat(String loserName) {
        System.out.println();
        System.out.println("=== FIM DE JOGO ===");
        System.out.println("DERROTA: a frota de " + loserName + " foi completamente afundada.");
    }

    public void printShotResult(String coord, domain.ShotResult result, String shipName) {
        switch (result) {
            case HIT           -> System.out.println("ACERTO em " + coord + "!");
            case SUNK          -> System.out.println("AFUNDOU " + (shipName != null ? shipName : "navio") + " (tiro em " + coord + ")!");
            case MISS          -> System.out.println("ÁGUA em " + coord + ".");
            case ALREADY_TRIED -> System.out.println("Você já atirou em " + coord + ".");
        }
    }

    public void printError(String message) {
        System.out.println("[ERRO] " + message);
    }

    public void printInfo(String message) {
        System.out.println(message);
    }

    // ------------------------------------------------------------------
    // Menu de ações do turno humano
    // ------------------------------------------------------------------

    public void printTurnMenu() {
        System.out.println("Ações: 1) Atirar   2) Ver log (últimos 10)   3) Meu tabuleiro");
        System.out.print("> ");
    }

    // ------------------------------------------------------------------
    // Coordenadas
    // ------------------------------------------------------------------

    /** "B7" a partir de índices internos. */
    public String prettyCoord(int x, int y) {
        return "" + (char)(colStart + x) + (y + 1);
    }

    /**
     * Converte string "B7" → {x, y} internos, ou null se inválida.
     * Exposto como utilitário para HumanPlayer / Game.
     */
    public int[] parseCoord(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(java.util.Locale.ROOT).replace(" ", "");
        if (t.length() < 2 || t.length() > 3) return null;

        char col = t.charAt(0);
        char colEnd = config.getColEnd();
        if (col < colStart || col > colEnd) return null;
        int x = col - colStart;

        int row;
        try { row = Integer.parseInt(t.substring(1)); }
        catch (NumberFormatException e) { return null; }
        if (row < 1 || row > boardSize) return null;

        return new int[]{x, row - 1};
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private String buildColumnHeader() {
        StringBuilder h = new StringBuilder("    ");
        for (int x = 0; x < boardSize; x++) {
            h.append((char)(colStart + x));
            if (x < boardSize - 1) h.append(' ');
        }
        return h.toString();
    }
}