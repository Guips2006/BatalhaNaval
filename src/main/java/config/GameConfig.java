package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class GameConfig {

    // Identificação do grupo
    private String groupId;
    private String groupName;
    private String version;

    // Tabuleiro
    private int boardSize;
    private char colStart;
    private char colEnd;

    // Frota
    private int[] fleetSizes;
    private String[] fleetNames;
    private AdjacencyRule adjacencyRule;

    // Regras de turno
    private boolean hitGrantsExtraShot;
    private int maxExtraShots;

    // CPU
    private CpuStrategy cpuStrategy;
    private boolean useParityPreference;

    // UI
    private boolean showOwnShips;
    private boolean showLegend;
    private int replayDelayMs;

    // Banco
    private boolean dbEnabled;
    private String dbType;
    private String dbSqliteFile;
    private boolean dbAutoMigrate;
    private boolean dbSaveInitialFleet;

    // Execução
    private Long seed;
    private GameMode gameMode;

    public static GameConfig load(String path) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel carregar " + path, e);
        }

        GameConfig c = new GameConfig();

        c.groupId    = props.getProperty("group.id");
        c.groupName  = props.getProperty("group.name");
        c.version    = props.getProperty("project.version");

        c.boardSize  = Integer.parseInt(props.getProperty("board.size"));
        c.colStart   = props.getProperty("board.columns.start").charAt(0);
        c.colEnd     = props.getProperty("board.columns.end").charAt(0);

        c.fleetSizes = Arrays.stream(props.getProperty("fleet.sizes").split(","))
                             .mapToInt(Integer::parseInt).toArray();
        c.fleetNames = props.getProperty("fleet.names").split(",");
        c.adjacencyRule = AdjacencyRule.valueOf(props.getProperty("fleet.adjacency_rule"));

        c.hitGrantsExtraShot = Boolean.parseBoolean(props.getProperty("rules.hit_grants_extra_shot"));
        c.maxExtraShots      = Integer.parseInt(props.getProperty("rules.max_extra_shots"));

        c.cpuStrategy        = CpuStrategy.valueOf(props.getProperty("cpu.strategy"));
        c.useParityPreference = Boolean.parseBoolean(props.getProperty("cpu.use_parity_preference"));

        c.showOwnShips  = Boolean.parseBoolean(props.getProperty("ui.show_own_ships"));
        c.showLegend    = Boolean.parseBoolean(props.getProperty("ui.show_legend"));
        c.replayDelayMs = Integer.parseInt(props.getProperty("ui.replay_delay_ms"));

        c.dbEnabled          = Boolean.parseBoolean(props.getProperty("db.enabled"));
        c.dbType             = props.getProperty("db.type");
        c.dbSqliteFile       = props.getProperty("db.sqlite.file");
        c.dbAutoMigrate      = Boolean.parseBoolean(props.getProperty("db.auto_migrate"));
        c.dbSaveInitialFleet = Boolean.parseBoolean(props.getProperty("db.save_initial_fleet"));

        String seedStr = props.getProperty("game.seed", "").trim();
        c.seed = seedStr.isEmpty() ? null : Long.parseLong(seedStr);

        c.gameMode = GameMode.valueOf(props.getProperty("game.mode"));

        return c;
    }

    public int getBoardSize()            { return boardSize; }
    public char getColStart()            { return colStart; }
    public char getColEnd()              { return colEnd; }
    public int[] getFleetSizes()         { return fleetSizes; }
    public String[] getFleetNames()      { return fleetNames; }
    public AdjacencyRule getAdjacencyRule() { return adjacencyRule; }
    public boolean isHitGrantsExtraShot(){ return hitGrantsExtraShot; }
    public int getMaxExtraShots()        { return maxExtraShots; }
    public CpuStrategy getCpuStrategy()  { return cpuStrategy; }
    public boolean isUseParityPreference(){ return useParityPreference; }
    public boolean isShowOwnShips()      { return showOwnShips; }
    public boolean isShowLegend()        { return showLegend; }
    public int getReplayDelayMs()        { return replayDelayMs; }
    public boolean isDbEnabled()         { return dbEnabled; }
    public String getDbSqliteFile()      { return dbSqliteFile; }
    public boolean isDbAutoMigrate()     { return dbAutoMigrate; }
    public boolean isDbSaveInitialFleet(){ return dbSaveInitialFleet; }
    public Long getSeed()                { return seed; }
    public GameMode getGameMode()        { return gameMode; }
    public String getGroupId()           { return groupId; }
}
