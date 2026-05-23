package domain;

import config.GameConfig;
import config.GameMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import persistence.MoveRepository;
import player.CpuPlayer;
import player.HumanPlayer;
import replay.ReplayService;
import ui.TerminalUI;
import validation.FleetValidator;
import validation.ValidationResult;

public final class Game {

    private final GameConfig    config;
    private final TerminalUI    ui;
    private final Scanner       scanner;
    private final FleetValidator validator;

    private HumanPlayer  human;
    private CpuPlayer    cpu;
    private Fleet        humanFleet;
    private Fleet        cpuFleet;

    private final List<String> log = new ArrayList<>();
    private String winner = null;
    private final List<persistence.MoveRepository.MoveEntry> moveEntries = new ArrayList<>();
    private int moveCounter = 0;

    public Game(GameConfig config, TerminalUI ui, Scanner scanner) {
        this.config    = config;
        this.ui        = ui;
        this.scanner   = scanner;
        this.validator = new FleetValidator(config);
    }

    public void run() {
        ui.printWelcome(config.getGroupId() + " v" + getVersion());

        GameMode mode = config.getGameMode();

        switch (mode) {
            case LIST   -> new ReplayService(config, ui).listMatches();
            case REPLAY -> runReplayMode();
            case PLAY   -> runPlayMode();
        }
    }

    
    private void runPlayMode() {
        initPlayers();
        placeFleets();
        gameLoop();
        postGame();
    }

    private void initPlayers() {
        ui.printInfo("Bem-vindo, Jogador!");
        human      = new HumanPlayer("Jogador", config, scanner);
        cpu        = new CpuPlayer("CPU", config);
        humanFleet = Fleet.fromConfig(config);
        cpuFleet   = Fleet.fromConfig(config);
    }

    private void placeFleets() {
        // Humano
        ui.printInfo("\n=== Posicionamento da sua frota ===");
        ui.printInfo("Colunas " + config.getColStart() + "-" + config.getColEnd()
                + ", linhas 1-" + config.getBoardSize() + ".");

        boolean valid = false;
        while (!valid) {
            human.placeFleet(config);
            humanFleet.markAllPlaced();

            ValidationResult vr = validator.validate(human.getBoard());
            if (vr.isValid()) {
                valid = true;
                log("Jogador posicionou a frota.");
            } else {
                ui.printError("Frota inválida:\n" + vr.summary());
                ui.printInfo("Tente posicionar novamente.");
                human.getBoard().reset();
            }
        }

        // CPU — posicionamento silencioso, sem validação de adjacência na UI
        cpu.placeFleet(config);
        cpuFleet.markAllPlaced();
        log("CPU posicionou a frota.");
    }

    private void gameLoop() {
        boolean playerTurn = true;

        while (true) {
            // Sincroniza frotas com o estado atual dos boards
            humanFleet.syncFromBoard(human.getBoard());
            cpuFleet.syncFromBoard(cpu.getBoard());

            // Exibe tabuleiros antes do turno do humano
            if (playerTurn) {
                ui.printInfo("");
                ui.printTwoBoards(human.getBoard(), human.getShotsBoard());
                ui.printTurnHeader("Jogador",
                        humanFleet.aliveCount(),
                        cpuFleet.aliveCount());
            }

            // Verifica fim de jogo
            if (cpuFleet.isDestroyed()) {
                winner = human.getName();
                ui.printVictory(winner);
                log("Fim: vitória do Jogador.");
                break;
            }
            if (humanFleet.isDestroyed()) {
                winner = cpu.getName();
                ui.printDefeat(human.getName());
                log("Fim: vitória da CPU.");
                break;
            }

            if (playerTurn) {
                playerTurn = !runHumanTurn();
            } else {
                runCpuTurn();
                playerTurn = true;
            }
        }
    }

    
    private boolean runHumanTurn() {
        ui.printInfo("\nSeu turno.");
        ui.printTurnMenu();
        String opt = scanner.nextLine().trim();

        switch (opt) {
            case "2" -> {
                ui.printLogTail(log, 10);
                return false; // continua no turno
            }
            case "3" -> {
                ui.printSingleBoard("SEU TABULEIRO", human.getBoard(), true);
                return false;
            }
            default -> {
                // Tiro
                return fireHuman();
            }
        }
    }

    private boolean fireHuman() {
        ui.printInfo("Coordenada para atirar (ex B7): ");
        String input = scanner.nextLine().trim();

        int[] xy = ui.parseCoord(input);
        if (xy == null) {
           ui.printError("Coordenada inválida.");
            return false;
        }

        if (human.getShotsBoard().hasTried(xy[0], xy[1])){
            ui.printError("Você já atirou em " + input.toUpperCase() + ".");
            return false;
        }

        ShotResult result = human.getShotsBoard().fireAt(cpu.getBoard(), xy[0], xy[1]);

   
        Ship justSunk = cpuFleet.detectNewlySunk(cpu.getBoard());
        cpuFleet.syncFromBoard(cpu.getBoard());

        String coord = ui.prettyCoord(xy[0], xy[1]);
        String shipName = justSunk != null ? justSunk.getName() : null;
        ui.printShotResult(coord, result, shipName);

        log("Jogador " + result.name().toLowerCase() + " em " + coord
                + (justSunk != null ? " (afundou " + justSunk.getName() + ")" : ""));

        moveEntries.add(new MoveRepository.MoveEntry(++moveCounter, human.getName(), coord, result));

        return true;
    }

    private void runCpuTurn() {
        ui.printInfo("\nTurno da CPU.");

        ShotResult result = cpu.takeTurn(human.getBoard(), config);

        String coord = ui.prettyCoord(cpu.getLastCoord()[0], cpu.getLastCoord()[1]);
        moveEntries.add(new MoveRepository.MoveEntry(++moveCounter, cpu.getName(), coord, result));

        Ship justSunk = humanFleet.detectNewlySunk(human.getBoard());
        humanFleet.syncFromBoard(human.getBoard());

        log("CPU " + result.name().toLowerCase()
                + (justSunk != null ? " — afundou " + justSunk.getName() : ""));

                // CpuPlayer precisa expor getLastCoord() — ver item abaixo
        int[] last = cpu.getLastCoord();
        if (last != null) {
            moveEntries.add(new persistence.MoveRepository.MoveEntry(++moveCounter, cpu.getName(), ui.prettyCoord(last[0], last[1]), result));
        }
}

    private void postGame() {
        ui.printInfo("");
        ui.printInfo("Mostrar log completo? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase(java.util.Locale.ROOT);
        if (resp.equals("s") || resp.equals("sim")) {
            ui.printFullLog(log);
        }
    }

    private void runReplayMode() {
        ReplayService replay = new ReplayService(config, ui);
        replay.listMatches();

        ui.printInfo("ID da partida para reproduzir: ");
        String idStr = scanner.nextLine().trim();
        try {
            long id = Long.parseLong(idStr);
            replay.replayMatch(id);
        } catch (NumberFormatException e) {
            ui.printError("ID inválido: " + idStr);
        }
    }

    public List<String> getLog() {
        return java.util.Collections.unmodifiableList(log);
    }

    /** Nome do vencedor, ou {@code null} se o jogo não terminou. */
    public String getWinner() {
        return winner;
    }

    /** Jogador humano (para persistência de dados do match). */
    public HumanPlayer getHuman() { return human; }

    /** Jogador CPU (para persistência). */
    public CpuPlayer getCpu() { return cpu; }

    private void log(String entry) {
        log.add(entry);
    }
    public List<MoveRepository.MoveEntry> getMoveEntries() {
        return Collections.unmodifiableList(moveEntries);
    }
    
    private String getVersion() {
        return "1.0";
    }
}
