// =========================================================
// persistence/MoveRepository.java
// =========================================================
package persistence;

import domain.ShotResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de jogadas (tabela {@code moves}).
 *
 * Cada tiro disparado (por humano ou CPU) gera um registro.
 * Sem lógica de domínio — apenas queries.
 */
public final class MoveRepository {

    private final DatabaseManager db;

    public MoveRepository(DatabaseManager db) {
        this.db = db;
    }

    // ------------------------------------------------------------------
    // Inserção
    // ------------------------------------------------------------------

    /**
     * Insere uma jogada.
     *
     * @param matchId     ID da partida
     * @param moveNumber  Número sequencial do turno (1-based)
     * @param actor       Nome do jogador que atirou
     * @param coord       Coordenada em formato legível ("B7")
     * @param result      Resultado do tiro
     * @return            ID do registro inserido
     */
    public long insert(long matchId, int moveNumber,
                       String actor, String coord, ShotResult result) throws SQLException {

        String sql = """
            INSERT INTO moves (match_id, move_number, actor, coord, result)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong  (1, matchId);
            ps.setInt   (2, moveNumber);
            ps.setString(3, actor);
            ps.setString(4, coord);
            ps.setString(5, result.name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("INSERT em moves não retornou ID.");
            }
        }
    }

    /**
     * Insere um lote de jogadas numa única transação.
     * Muito mais eficiente do que inserir uma a uma ao final da partida.
     *
     * @param matchId  ID da partida
     * @param moves    Lista de jogadas na ordem cronológica
     */
    public void insertBatch(long matchId, List<MoveEntry> moves) throws SQLException {
        if (moves == null || moves.isEmpty()) return;

        String sql = """
            INSERT INTO moves (match_id, move_number, actor, coord, result)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (MoveEntry m : moves) {
                ps.setLong  (1, matchId);
                ps.setInt   (2, m.moveNumber);
                ps.setString(3, m.actor);
                ps.setString(4, m.coord);
                ps.setString(5, m.result.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * Retorna todas as jogadas de uma partida em ordem cronológica.
     * Usado pelo ReplayService.
     */
    public List<MoveRecord> findByMatch(long matchId) throws SQLException {
        String sql = """
            SELECT id, move_number, actor, coord, result
            FROM moves
            WHERE match_id = ?
            ORDER BY move_number ASC
            """;

        List<MoveRecord> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MoveRecord(
                        rs.getLong  ("id"),
                        matchId,
                        rs.getInt   ("move_number"),
                        rs.getString("actor"),
                        rs.getString("coord"),
                        rs.getString("result")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Conta o total de jogadas de uma partida (útil para estatísticas).
     */
    public int countByMatch(long matchId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM moves WHERE match_id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    /**
     * Entrada para inserção em lote — criada pelo Game ao acumular jogadas.
     */
    public static final class MoveEntry {
        public final int        moveNumber;
        public final String     actor;
        public final String     coord;
        public final ShotResult result;

        public MoveEntry(int moveNumber, String actor, String coord, ShotResult result) {
            this.moveNumber = moveNumber;
            this.actor      = actor;
            this.coord      = coord;
            this.result     = result;
        }
    }

    /**
     * Projeção de uma jogada lida do banco.
     */
    public static final class MoveRecord {
        public final long   id;
        public final long   matchId;
        public final int    moveNumber;
        public final String actor;
        public final String coord;
        public final String result;   // nome do enum ShotResult

        public MoveRecord(long id, long matchId, int moveNumber,
                          String actor, String coord, String result) {
            this.id         = id;
            this.matchId    = matchId;
            this.moveNumber = moveNumber;
            this.actor      = actor;
            this.coord      = coord;
            this.result     = result;
        }

        @Override
        public String toString() {
            return String.format("Move{#%d %s → %s (%s)}", moveNumber, actor, coord, result);
        }
    }
}