// =========================================================
// persistence/MatchRepository.java
// =========================================================
package persistence;

import config.GameConfig;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de partidas (tabela {@code matches}).
 *
 * Cada instância usa a conexão gerenciada pelo DatabaseManager.
 * Sem lógica de domínio — apenas queries.
 */
public final class MatchRepository {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseManager db;

    public MatchRepository(DatabaseManager db) {
        this.db = db;
    }

    // ------------------------------------------------------------------
    // Inserção
    // ------------------------------------------------------------------

    /**
     * Insere uma nova partida com status "em andamento" e retorna o ID gerado.
     *
     * @param config      Configuração do jogo (group_id, cpu_strategy, board_size, seed)
     * @param playerName  Nome do jogador humano
     * @return            ID da partida recém-criada
     */
    public long insert(GameConfig config, String playerName) throws SQLException {
        String sql = """
            INSERT INTO matches
                (group_id, player_name, cpu_strategy, board_size, seed, started_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, config.getGroupId());
            ps.setString(2, playerName);
            ps.setString(3, config.getCpuStrategy().name());
            ps.setInt   (4, config.getBoardSize());
            ps.setString(5, config.getSeed() != null ? config.getSeed().toString() : null);
            ps.setString(6, now());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("INSERT em matches não retornou ID.");
            }
        }
    }

    /**
     * Finaliza uma partida gravando o vencedor e a hora de fim.
     *
     * @param matchId  ID da partida
     * @param winner   Nome do vencedor (null se partida abandonada)
     */
    public void finish(long matchId, String winner) throws SQLException {
        String sql = """
            UPDATE matches
               SET winner = ?, finished_at = ?
             WHERE id = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, winner);
            ps.setString(2, now());
            ps.setLong  (3, matchId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * Retorna todas as partidas, da mais recente para a mais antiga.
     * Usado pelo ReplayService (modo LIST).
     */
    public List<MatchRecord> findAll() throws SQLException {
        String sql = """
            SELECT id, player_name, cpu_strategy, board_size, winner, played_at
            FROM matches
            ORDER BY played_at DESC
            """;

        List<MatchRecord> list = new ArrayList<>();
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new MatchRecord(
                    rs.getLong  ("id"),
                    rs.getString("player_name"),
                    rs.getString("cpu_strategy"),
                    rs.getInt   ("board_size"),
                    rs.getString("winner"),
                    rs.getString("played_at")
                ));
            }
        }
        return list;
    }

    /**
     * Busca uma partida pelo ID. Retorna null se não encontrada.
     */
    public MatchRecord findById(long matchId) throws SQLException {
        String sql = """
            SELECT id, player_name, cpu_strategy, board_size, winner, played_at
            FROM matches WHERE id = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new MatchRecord(
                    rs.getLong  ("id"),
                    rs.getString("player_name"),
                    rs.getString("cpu_strategy"),
                    rs.getInt   ("board_size"),
                    rs.getString("winner"),
                    rs.getString("played_at")
                );
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String now() {
        return LocalDateTime.now().format(FMT);
    }

    // ------------------------------------------------------------------
    // DTO
    // ------------------------------------------------------------------

    /**
     * Projeção plana de uma partida — sem dependência de domínio.
     * Usado pelo ReplayService e pela UI de listagem.
     */
    public static final class MatchRecord {
        public final long   id;
        public final String playerName;
        public final String cpuStrategy;
        public final int    boardSize;
        public final String winner;
        public final String playedAt;

        public MatchRecord(long id, String playerName, String cpuStrategy,
                           int boardSize, String winner, String playedAt) {
            this.id          = id;
            this.playerName  = playerName;
            this.cpuStrategy = cpuStrategy;
            this.boardSize   = boardSize;
            this.winner      = winner;
            this.playedAt    = playedAt;
        }

        @Override
        public String toString() {
            return String.format("Match{id=%d player='%s' winner='%s' at='%s'}",
                    id, playerName, winner, playedAt);
        }
    }
}