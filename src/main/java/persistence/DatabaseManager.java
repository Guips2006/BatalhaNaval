// =========================================================
// persistence/DatabaseManager.java
// =========================================================
package persistence;

import config.GameConfig;
import java.io.File;
import java.sql.*;

/**
 * Gerencia o ciclo de vida da conexão SQLite e o schema do banco.
 *
 * Responsabilidades:
 *  - Abrir / fechar conexão com o arquivo .db configurado.
 *  - Criar as tabelas (auto-migrate) se db.auto_migrate=true.
 *  - Fornecer a conexão ativa para os repositórios.
 *
 * Nenhum SQL de negócio aqui — apenas DDL e lifecycle.
 * Sem System.out — erros são propagados como SQLException ou RuntimeException.
 */
public final class DatabaseManager implements AutoCloseable {

    // ------------------------------------------------------------------
    // DDL — schema completo
    // ------------------------------------------------------------------

    /**
     * matches — uma linha por partida.
     * Colunas extras (player_name, cpu_strategy, board_size) são redundantes
     * mas evitam JOINs no ReplayService e no LIST de partidas.
     */
    private static final String SQL_CREATE_MATCHES = """
        CREATE TABLE IF NOT EXISTS matches (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            group_id     TEXT    NOT NULL,
            player_name  TEXT    NOT NULL,
            cpu_strategy TEXT    NOT NULL,
            board_size   INTEGER NOT NULL,
            seed         TEXT,
            winner       TEXT,
            started_at   TEXT    NOT NULL,
            finished_at  TEXT,
            played_at    TEXT    GENERATED ALWAYS AS (
                             COALESCE(finished_at, started_at)
                         ) STORED
        )
        """;

    /**
     * players — dois registros por partida (humano + cpu).
     */
    private static final String SQL_CREATE_PLAYERS = """
        CREATE TABLE IF NOT EXISTS players (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            match_id  INTEGER NOT NULL REFERENCES matches(id),
            name      TEXT    NOT NULL,
            type      TEXT    NOT NULL CHECK(type IN ('HUMAN','CPU'))
        )
        """;

    /**
     * moves — uma linha por tiro disparado.
     * actor  = nome do jogador que atirou.
     * coord  = "B7" (label legível).
     * result = MISS | HIT | SUNK | ALREADY_TRIED.
     */
    private static final String SQL_CREATE_MOVES = """
        CREATE TABLE IF NOT EXISTS moves (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            match_id    INTEGER NOT NULL REFERENCES matches(id),
            move_number INTEGER NOT NULL,
            actor       TEXT    NOT NULL,
            coord       TEXT    NOT NULL,
            result      TEXT    NOT NULL
        )
        """;

    /**
     * fleet_initial — posicionamento inicial de cada navio (opcional,
     * controlado por db.save_initial_fleet). Permite replay completo com
     * reconstituição do tabuleiro.
     */
    private static final String SQL_CREATE_FLEET_INITIAL = """
        CREATE TABLE IF NOT EXISTS fleet_initial (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            match_id    INTEGER NOT NULL REFERENCES matches(id),
            player_type TEXT    NOT NULL CHECK(player_type IN ('HUMAN','CPU')),
            ship_index  INTEGER NOT NULL,
            ship_name   TEXT    NOT NULL,
            ship_size   INTEGER NOT NULL,
            origin_x    INTEGER NOT NULL,
            origin_y    INTEGER NOT NULL,
            horizontal  INTEGER NOT NULL CHECK(horizontal IN (0,1))
        )
        """;

    // Índices para acelerar as queries do ReplayService
    private static final String SQL_IDX_MOVES_MATCH =
        "CREATE INDEX IF NOT EXISTS idx_moves_match_id ON moves(match_id)";
    private static final String SQL_IDX_PLAYERS_MATCH =
        "CREATE INDEX IF NOT EXISTS idx_players_match_id ON players(match_id)";

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    private final GameConfig config;
    private Connection       connection;

    // ------------------------------------------------------------------
    // Construção e lifecycle
    // ------------------------------------------------------------------

    public DatabaseManager(GameConfig config) {
        this.config = config;
    }

    /**
     * Abre a conexão e, se db.auto_migrate=true, cria tabelas ausentes.
     * Idempotente: pode ser chamado mais de uma vez sem duplicar tabelas.
     */
    public void init() {
        if (!config.isDbEnabled()) return;

        ensureDirectoryExists();

        try {
            // Carrega o driver explicitamente (necessário com sqlite-jdbc-3.36)
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + config.getDbSqliteFile());

            // WAL mode melhora concorrência e performance de escrita
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
            }

            if (config.isDbAutoMigrate()) {
                migrate();
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Driver SQLite não encontrado. Verifique se sqlite-jdbc-3.36.0.3.jar está no classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inicializar o banco de dados: " + e.getMessage(), e);
        }
    }

    /** Cria todas as tabelas e índices (IF NOT EXISTS — seguro re-executar). */
    private void migrate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(SQL_CREATE_MATCHES);
            st.execute(SQL_CREATE_PLAYERS);
            st.execute(SQL_CREATE_MOVES);
            st.execute(SQL_CREATE_FLEET_INITIAL);
            st.execute(SQL_IDX_MOVES_MATCH);
            st.execute(SQL_IDX_PLAYERS_MATCH);
        }
    }

    private void ensureDirectoryExists() {
        String path = config.getDbSqliteFile();
        File   file = new File(path);
        File   dir  = file.getParentFile();
        if (dir != null && !dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Não foi possível criar o diretório: " + dir.getAbsolutePath());
            }
        }
    }

    // ------------------------------------------------------------------
    // Acesso à conexão (package-private — só repositórios usam)
    // ------------------------------------------------------------------

    /**
     * Conexão ativa. Nunca null após {@link #init()} quando db.enabled=true.
     * Repositórios não devem fechar esta conexão.
     */
    Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException(
                "DatabaseManager não foi inicializado. Chame init() primeiro.");
        }
        return connection;
    }

    /** True se o banco está habilitado e a conexão foi aberta. */
    public boolean isReady() {
        try {
            return config.isDbEnabled()
                && connection != null
                && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Transações — helpers para repositórios
    // ------------------------------------------------------------------

    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    public void commit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {}
    }

    // ------------------------------------------------------------------
    // AutoCloseable
    // ------------------------------------------------------------------

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}