// =========================================================
// replay/ReplayService.java
// =========================================================
package replay;

import config.GameConfig;
import ui.TerminalUI;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia listagem e reprodução de partidas salvas no SQLite.
 *
 * Usa as mesmas tabelas criadas por DatabaseManager:
 *   matches  (id, player_name, cpu_strategy, board_size, winner, played_at)
 *   moves    (id, match_id, move_number, actor, coord, result)
 *
 * Modos de execução disparados por GameMode:
 *   LIST   — imprime tabela de partidas salvas e termina.
 *   REPLAY — pede um ID ao usuário e reproduz os movimentos no terminal.
 */
public class ReplayService {

    private final GameConfig config;
    private final TerminalUI ui;

    public ReplayService(GameConfig config, TerminalUI ui) {
        this.config = config;
        this.ui     = ui;
    }

    // ------------------------------------------------------------------
    // LIST
    // ------------------------------------------------------------------

    /**
     * Imprime todas as partidas salvas, ordenadas da mais recente para a mais antiga.
     */
    public void listMatches() {
        if (!config.isDbEnabled()) {
            ui.printInfo("Banco de dados desabilitado (db.enabled=false).");
            return;
        }

        try (Connection conn = openConnection()) {
            String sql = """
                SELECT id, player_name, cpu_strategy, board_size, winner, played_at
                FROM matches
                ORDER BY played_at DESC
                """;

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new String[]{
                        String.valueOf(rs.getLong("id")),
                        rs.getString("player_name"),
                        rs.getString("cpu_strategy"),
                        String.valueOf(rs.getInt("board_size")),
                        rs.getString("winner"),
                        rs.getString("played_at")
                    });
                }

                if (rows.isEmpty()) {
                    ui.printInfo("Nenhuma partida salva ainda.");
                    return;
                }

                printMatchTable(rows);
            }

        } catch (SQLException e) {
            ui.printError("Erro ao listar partidas: " + e.getMessage());
        }
    }

    private void printMatchTable(List<String[]> rows) {
        System.out.printf("%n%-6s %-15s %-10s %-6s %-15s %-20s%n",
                "ID", "Jogador", "Estratégia", "Board", "Vencedor", "Data");
        System.out.println("-".repeat(76));
        for (String[] r : rows) {
            System.out.printf("%-6s %-15s %-10s %-6s %-15s %-20s%n",
                    r[0], r[1], r[2], r[3], r[4], r[5]);
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // REPLAY
    // ------------------------------------------------------------------

    /**
     * Reproduz uma partida salva, imprimindo cada movimento com pausa configurável.
     *
     * @param matchId ID da partida (lido de stdin pelo Game antes de chamar aqui,
     *                ou passado diretamente em testes)
     */
    public void replayMatch(long matchId) {
        if (!config.isDbEnabled()) {
            ui.printInfo("Banco de dados desabilitado (db.enabled=false).");
            return;
        }

        try (Connection conn = openConnection()) {
            MatchHeader header = loadHeader(conn, matchId);
            if (header == null) {
                ui.printInfo("Partida #" + matchId + " não encontrada.");
                return;
            }

            List<MoveRecord> moves = loadMoves(conn, matchId);

            printReplayHeader(header, moves.size());
            replayMoves(moves);
            printReplayFooter(header);

        } catch (SQLException e) {
            ui.printError("Erro ao reproduzir partida: " + e.getMessage());
        }
    }

    private void printReplayHeader(MatchHeader h, int totalMoves) {
        System.out.println();
        System.out.println("=== REPLAY — Partida #" + h.id + " ===");
        System.out.printf("Jogador: %-15s  Estratégia CPU: %s%n", h.playerName, h.cpuStrategy);
        System.out.printf("Tabuleiro: %dx%d          Movimentos: %d%n",
                h.boardSize, h.boardSize, totalMoves);
        System.out.printf("Data: %s%n%n", h.playedAt);
    }

    private void replayMoves(List<MoveRecord> moves) {
        int delayMs = config.getReplayDelayMs();
        for (MoveRecord m : moves) {
            System.out.printf("[%3d] %-8s → %s  (%s)%n",
                    m.moveNumber, m.actor, m.coord, m.result);
            pause(delayMs);
        }
    }

    private void pause(int delayMs) {
        if (delayMs <= 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printReplayFooter(MatchHeader h) {
        System.out.println();
        System.out.println("Resultado: " + h.winner + " venceu.");
        System.out.println("=== FIM DO REPLAY ===");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    private MatchHeader loadHeader(Connection conn, long matchId) throws SQLException {
        String sql = """
            SELECT id, player_name, cpu_strategy, board_size, winner, played_at
            FROM matches WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new MatchHeader(
                    rs.getLong("id"),
                    rs.getString("player_name"),
                    rs.getString("cpu_strategy"),
                    rs.getInt("board_size"),
                    rs.getString("winner"),
                    rs.getString("played_at")
                );
            }
        }
    }

    private List<MoveRecord> loadMoves(Connection conn, long matchId) throws SQLException {
        String sql = """
            SELECT move_number, actor, coord, result
            FROM moves
            WHERE match_id = ?
            ORDER BY move_number ASC
            """;
        List<MoveRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MoveRecord(
                        rs.getInt("move_number"),
                        rs.getString("actor"),
                        rs.getString("coord"),
                        rs.getString("result")
                    ));
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Conexão
    // ------------------------------------------------------------------

    private Connection openConnection() throws SQLException {
        String url = "jdbc:sqlite:" + config.getDbSqliteFile();
        return DriverManager.getConnection(url);
    }

    // ------------------------------------------------------------------
    // Tipos internos (records leves)
    // ------------------------------------------------------------------

    private static final class MatchHeader {
        final long   id;
        final String playerName;
        final String cpuStrategy;
        final int    boardSize;
        final String winner;
        final String playedAt;

        MatchHeader(long id, String playerName, String cpuStrategy,
                    int boardSize, String winner, String playedAt) {
            this.id          = id;
            this.playerName  = playerName;
            this.cpuStrategy = cpuStrategy;
            this.boardSize   = boardSize;
            this.winner      = winner;
            this.playedAt    = playedAt;
        }
    }

    private static final class MoveRecord {
        final int    moveNumber;
        final String actor;
        final String coord;
        final String result;

        MoveRecord(int moveNumber, String actor, String coord, String result) {
            this.moveNumber = moveNumber;
            this.actor      = actor;
            this.coord      = coord;
            this.result     = result;
        }
    }
}