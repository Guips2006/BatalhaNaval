// =========================================================
// player/CpuPlayer.java
// =========================================================
package player;

import java.util.ArrayDeque;
import java.util.Random;

import config.CpuStrategy;
import config.GameConfig;
import domain.Board;
import domain.ShotResult;

/**
 * Jogador CPU com três estratégias configuráveis:
 *
 *  RANDOM  — tiro completamente aleatório.
 *  HUNT    — aleatório até acertar; depois explora os vizinhos ortogonais.
 *  PARITY  — preferência por células onde (x+y) é par antes de caçar.
 *
 * A estratégia é lida de GameConfig; não precisa de subclasses.
 */
public class CpuPlayer extends Player {

    private final Random             rng;
    private final CpuStrategy        strategy;
    private final boolean            parityPreference;

    // Estado interno da IA de caça
    private final ArrayDeque<int[]>  targets  = new ArrayDeque<>();
    private final boolean[][]        tried;   // inicializado no construtor
    private int[] lastCoord;


    public CpuPlayer(String name, GameConfig config) {
        super(name, config);
        this.strategy        = config.getCpuStrategy();
        this.parityPreference = config.isUseParityPreference();
        this.rng             = config.getSeed() != null
                ? new Random(config.getSeed() ^ 0xDEADBEEFL) // seed distinta do humano
                : new Random();
        this.tried = new boolean[config.getBoardSize()][config.getBoardSize()];
    }

    // ------------------------------------------------------------------
    // Posicionamento automático
    // ------------------------------------------------------------------

    @Override
public void placeFleet(GameConfig config) {
    int[] sizes = config.getFleetSizes();
    int   n     = config.getBoardSize();
    config.AdjacencyRule rule = config.getAdjacencyRule();

    for (int sid = 0; sid < sizes.length; sid++) {
        boolean ok = false;
        int attempts = 0;
        while (!ok && attempts < 5000) {
            attempts++;
            boolean horiz = rng.nextBoolean();
            int x = rng.nextInt(n);
            int y = rng.nextInt(n);
            if (!board.canPlaceShip(x, y, sizes[sid], horiz)) continue;
            if (wouldViolateAdjacency(x, y, sizes[sid], horiz, n, rule)) continue;
            board.placeShip(x, y, sizes[sid], horiz, sid);
            ok = true;
        }
        if (!ok) throw new IllegalStateException(
                "CPU não conseguiu posicionar o navio " + sid);
    }
}

private boolean wouldViolateAdjacency(int x, int y, int size, boolean horiz,
                                       int n, config.AdjacencyRule rule) {
    if (rule == config.AdjacencyRule.NONE) return false;

    for (int i = 0; i < size; i++) {
        int cx = horiz ? x + i : x;
        int cy = horiz ? y     : y + i;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (rule == config.AdjacencyRule.ORTHO
                        && Math.abs(dx) + Math.abs(dy) > 1) continue;
                int nx = cx + dx, ny = cy + dy;
                if (nx < 0 || nx >= n || ny < 0 || ny >= n) continue;
                if (board.getShipCell(nx, ny) == domain.Board.SHIP) return true;
            }
        }
    }
    return false;
}    // ------------------------------------------------------------------
    // Turno
    // ------------------------------------------------------------------

    @Override
    public ShotResult takeTurn(Board opponent, GameConfig config) {
        ShotResult last = null;
        int extraShots = 0;
        int maxExtra   = config.isHitGrantsExtraShot() ? config.getMaxExtraShots() : 0;

        do {
            lastCoord = chooseTarget(config.getBoardSize());
            int[] coord = lastCoord;
            markTried(coord[0], coord[1]);
            
            lastCoord = coord;
            last = board.fireAt(opponent, coord[0], coord[1]);

            String pos = prettyCoord(coord[0], coord[1], config.getColStart());
            switch (last) {
                case HIT  -> { System.out.println("CPU acertou em " + pos + "!"); enqueueNeighbors(coord[0], coord[1], config.getBoardSize()); }
                case SUNK -> { System.out.println("CPU AFUNDOU seu navio! (tiro em " + pos + ")"); clearTargetsOnSunk(); }
                case MISS -> System.out.println("CPU errou em " + pos + ".");
                case ALREADY_TRIED -> { /* não deve ocorrer — already guarded */ }
            }

            if (last == ShotResult.HIT || last == ShotResult.SUNK) extraShots++;
            else break;

        } while (config.isHitGrantsExtraShot()
                && extraShots <= maxExtra
                && opponent.remainingShips() > 0);

        return last;
    }

    public int[] getLastCoord() { return lastCoord; }
    // ------------------------------------------------------------------
    // Lógica de escolha de alvo
    // ------------------------------------------------------------------

    private int[] chooseTarget(int n) {
        // 1. Fila de caça (HUNT / PARITY)
        if (strategy != CpuStrategy.RANDOM) {
            while (!targets.isEmpty()) {
                int[] t = targets.removeFirst();
                if (isValid(t[0], t[1], n) && !tried[t[1]][t[0]]) return t;
            }
        }

        // 2. Varredura aleatória
        return randomUntried(n);
    }

    private int[] randomUntried(int n) {
        // Tenta até 5000 vezes com preferência de paridade (se configurado)
        for (int attempt = 0; attempt < 5000; attempt++) {
            int x = rng.nextInt(n);
            int y = rng.nextInt(n);
            if (tried[y][x]) continue;
            boolean parityMatch = (x + y) % 2 == 0;
            if (!parityPreference || parityMatch || rng.nextInt(4) == 0) {
                return new int[]{x, y};
            }
        }
        // Fallback sequencial
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++)
                if (!tried[y][x]) return new int[]{x, y};

        throw new IllegalStateException("CPU: sem células disponíveis — o tabuleiro está esgotado.");
    }

    private void enqueueNeighbors(int x, int y, int n) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (isValid(nx, ny, n) && !tried[ny][nx]) targets.addLast(new int[]{nx, ny});
        }
    }

    /** Quando afunda, descarta fila de caça com probabilidade configurável (60 %). */
    private void clearTargetsOnSunk() {
        if (rng.nextInt(100) < 60) targets.clear();
    }

    private void markTried(int x, int y) { tried[y][x] = true; }

    private static boolean isValid(int x, int y, int n) {
        return x >= 0 && x < n && y >= 0 && y < n;
    }

    private static String prettyCoord(int x, int y, char colStart) {
        return "" + (char)(colStart + x) + (y + 1);
    }

}