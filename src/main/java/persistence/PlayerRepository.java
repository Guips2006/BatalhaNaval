// =========================================================
// persistence/PlayerRepository.java
// =========================================================
package persistence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de jogadores (tabela {@code players}).
 *
 * Dois registros são inseridos por partida: um HUMAN e um CPU.
 * Sem lógica de domínio — apenas queries.
 */
public final class PlayerRepository {

    private final DatabaseManager db;

    public PlayerRepository(DatabaseManager db) {
        this.db = db;
    }

    // ------------------------------------------------------------------
    // Inserção
    // ------------------------------------------------------------------

    /**
     * Insere um jogador vinculado a uma partida.
     *
     * @param matchId  ID da partida
     * @param name     Nome do jogador
     * @param type     "HUMAN" ou "CPU"
     * @return         ID do registro inserido
     */
    public long insert(long matchId, String name, String type) throws SQLException {
        validateType(type);

        String sql = "INSERT INTO players (match_id, name, type) VALUES (?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong  (1, matchId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("INSERT em players não retornou ID.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * Retorna todos os jogadores de uma partida (normalmente 2).
     */
    public List<PlayerRecord> findByMatch(long matchId) throws SQLException {
        String sql = "SELECT id, match_id, name, type FROM players WHERE match_id = ?";

        List<PlayerRecord> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerRecord(
                        rs.getLong  ("id"),
                        rs.getLong  ("match_id"),
                        rs.getString("name"),
                        rs.getString("type")
                    ));
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void validateType(String type) {
        if (!"HUMAN".equals(type) && !"CPU".equals(type)) {
            throw new IllegalArgumentException("Tipo inválido: " + type + ". Use 'HUMAN' ou 'CPU'.");
        }
    }

    // ------------------------------------------------------------------
    // DTO
    // ------------------------------------------------------------------

    public static final class PlayerRecord {
        public final long   id;
        public final long   matchId;
        public final String name;
        public final String type;

        public PlayerRecord(long id, long matchId, String name, String type) {
            this.id      = id;
            this.matchId = matchId;
            this.name    = name;
            this.type    = type;
        }

        @Override
        public String toString() {
            return String.format("Player{id=%d match=%d name='%s' type=%s}",
                    id, matchId, name, type);
        }
    }
}