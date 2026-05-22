package player;

import java.util.Locale;
import java.util.Scanner;

import config.GameConfig;
import domain.Board;
import domain.ShotResult;

public class HumanPlayer extends Player {

    private final Scanner scanner;

    public HumanPlayer(String name, GameConfig config, Scanner scanner) {
        super(name, config);
        this.scanner = scanner;
    }

    @Override
    public void placeFleet(GameConfig config) {
        int[]    sizes    = config.getFleetSizes();
        String[] names    = config.getFleetNames();
        char     colStart = config.getColStart();
        char     colEnd   = config.getColEnd();
        int      n        = config.getBoardSize();

        System.out.println();
        System.out.print("Deseja posicionar manualmente? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

        if (resp.equals("s") || resp.equals("sim")) {
            placeManually(sizes, names, colStart, colEnd, n);
        } else {
            placeRandomly(sizes, config);
            System.out.println("Frota posicionada automaticamente.");
        }
    }

    private void placeManually(int[] sizes, String[] names,
                               char colStart, char colEnd, int n) {
        for (int sid = 0; sid < sizes.length; sid++) {
            boolean placed = false;
            while (!placed) {
                printOwnBoard(colStart);
                System.out.println("Posicione: " + names[sid] + " (tamanho " + sizes[sid] + ")");
                System.out.print("Coordenada inicial (ex A1): ");
                String coordStr = scanner.nextLine().trim();

                int[] xy = parseCoord(coordStr, colStart, colEnd, n);
                if (xy == null) { System.out.println("Coordenada inválida."); continue; }

                System.out.print("Direção (H horizontal / V vertical): ");
                String dir = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
                boolean horiz = dir.equals("H");
                boolean vert  = dir.equals("V");
                if (!horiz && !vert) { System.out.println("Direção inválida."); continue; }

                if (!board.canPlaceShip(xy[0], xy[1], sizes[sid], horiz)) {
                    System.out.println("Não cabe ou colide.");
                    continue;
                }

                board.placeShip(xy[0], xy[1], sizes[sid], horiz, sid);
                System.out.println(names[sid] + " posicionado.");
                placed = true;
            }
        }
    }

    public void placeRandomly(int[] sizes, GameConfig config) {
        java.util.Random rng = config.getSeed() != null
                ? new java.util.Random(config.getSeed())
                : new java.util.Random();
        int n = config.getBoardSize();
        config.AdjacencyRule rule = config.getAdjacencyRule();

        for (int sid = 0; sid < sizes.length; sid++) {
            boolean ok = false;
            int tries = 0;
            while (!ok && tries < 5000) {
                tries++;
                boolean horiz = rng.nextBoolean();
                int x = rng.nextInt(n);
                int y = rng.nextInt(n);
                if (!board.canPlaceShip(x, y, sizes[sid], horiz)) continue;
                if (wouldViolateAdjacency(x, y, sizes[sid], horiz, n, rule)) continue;
                board.placeShip(x, y, sizes[sid], horiz, sid);
                ok = true;
            }
            if (!ok) throw new IllegalStateException(
                    "Não foi possível posicionar automaticamente o navio " + sid);
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
    }

    @Override
    public ShotResult takeTurn(Board opponent, GameConfig config) {
        ShotResult last = null;
        int extraShots = 0;
        int maxExtra   = config.isHitGrantsExtraShot() ? config.getMaxExtraShots() : 0;

        do {
            if (last != null) {
                System.out.println("ACERTO! Você ganhou um tiro extra (" +
                        (maxExtra - extraShots) + " restante(s)).");
            }

            last = shootOnce(opponent, config);

            if (last == ShotResult.HIT || last == ShotResult.SUNK) {
                extraShots++;
            } else {
                break;
            }

        } while (config.isHitGrantsExtraShot()
                && extraShots <= maxExtra
                && opponent.remainingShips() > 0);

        return last;
    }

    private ShotResult shootOnce(Board opponent, GameConfig config) {
        char colStart = config.getColStart();
        char colEnd   = config.getColEnd();
        int  n        = config.getBoardSize();

        while (true) {
            System.out.print("Coordenada para atirar (ex B7): ");
            String input = scanner.nextLine().trim();
            int[] xy = parseCoord(input, colStart, colEnd, n);

            if (xy == null) { System.out.println("Inválida."); continue; }
            if (board.hasTried(xy[0], xy[1])) {
                System.out.println("Você já atirou aí.");
                continue;
            }

            ShotResult result = board.fireAt(opponent, xy[0], xy[1]);

            switch (result) {
                case HIT           -> System.out.println("ACERTO!");
                case SUNK          -> System.out.println("AFUNDOU um navio inimigo!");
                case MISS          -> System.out.println("ÁGUA.");
                case ALREADY_TRIED -> System.out.println("Você já atirou aí.");
            }
            return result;
        }
    }

    private void printOwnBoard(char colStart) {
        System.out.println();
        System.out.println("SEU TABULEIRO:");
        int size = board.getSize();
        StringBuilder header = new StringBuilder("    ");
        for (int x = 0; x < size; x++) {
            header.append((char)(colStart + x));
            if (x < size - 1) header.append(' ');
        }
        System.out.println(header);
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder(String.format("%2d  ", y + 1));
            for (int x = 0; x < size; x++) row.append(board.getShipCell(x, y)).append(' ');
            System.out.println(row);
        }
    }

    static int[] parseCoord(String s, char colStart, char colEnd, int n) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (t.length() < 2 || t.length() > 3) return null;

        char col = t.charAt(0);
        if (col < colStart || col > colEnd) return null;
        int x = col - colStart;

        int row;
        try { row = Integer.parseInt(t.substring(1)); }
        catch (NumberFormatException e) { return null; }

        if (row < 1 || row > n) return null;
        return new int[]{x, row - 1};
    }
}