package domain;

import config.GameConfig;
import java.util.Arrays;

public class Board {
    public static final char EMPTY = '.';
    public static final char SHIP = 'S';
    public static final char HIT = 'X';
    public static final char MISS = 'o';

    private final int size;
    private final char[][] ships;
    private final char[][] shots;
    private final int[][] shipId;
    private final int[] shipHp;

    public Board(GameConfig config) {
        this(config.getBoardSize(), config.getFleetSizes());
    }

    public Board(int size, int[] fleetSizes) {
        if (size <= 0) {
            throw new IllegalArgumentException("Board size must be a positive number");
        }
        if (fleetSizes == null || fleetSizes.length == 0) {
            throw new IllegalArgumentException("fleetSizes must not be null or empty");
        }

        this.size = size;
        this.ships = new char[size][size];
        this.shots = new char[size][size];
        this.shipId = new int[size][size];
        this.shipHp = Arrays.copyOf(fleetSizes, fleetSizes.length);

        fillGrid(ships, EMPTY);
        fillGrid(shots, EMPTY);
        fillShipIds(-1);
    }

    public static Board fromConfig(GameConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("GameConfig must not be null");
        }
        return new Board(config.getBoardSize(), config.getFleetSizes());
    }

    public int getSize() {
        return size;
    }

    public void reset() {
        fillGrid(ships, EMPTY);
        fillGrid(shots, EMPTY);
        fillShipIds(-1);
    }

    public boolean canPlaceShip(int x, int y, int length, boolean horizontal) {
        validateCoordinates(x, y);
        if (length <= 0) {
            return false;
        }

        if (horizontal) {
            if (x + length > size) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (ships[y][x + i] != EMPTY) {
                    return false;
                }
            }
        } else {
            if (y + length > size) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (ships[y + i][x] != EMPTY) {
                    return false;
                }
            }
        }

        return true;
    }

    public void placeShip(int x, int y, int length, boolean horizontal, int shipIndex) {
        if (!canPlaceShip(x, y, length, horizontal)) {
            throw new IllegalArgumentException("Cannot place ship at the requested position");
        }
        if (shipIndex < 0 || shipIndex >= shipHp.length) {
            throw new IllegalArgumentException("Invalid ship index");
        }

        if (horizontal) {
            for (int i = 0; i < length; i++) {
                ships[y][x + i] = SHIP;
                shipId[y][x + i] = shipIndex;
            }
        } else {
            for (int i = 0; i < length; i++) {
                ships[y + i][x] = SHIP;
                shipId[y + i][x] = shipIndex;
            }
        }
    }

    public ShotResult fireAt(Board opponent, int x, int y) {
        if (opponent == null) {
            throw new IllegalArgumentException("Opponent board must not be null");
        }
        validateCoordinates(x, y);
        if (shots[y][x] != EMPTY) {
            return ShotResult.ALREADY_TRIED;
        }

        ShotResult result = opponent.receiveShot(x, y);
        shots[y][x] = result == ShotResult.MISS ? MISS : HIT;
        return result;
    }

    public int remainingShips() {
        int count = 0;
        for (int hp : shipHp) {
            if (hp > 0) {
                count++;
            }
        }
        return count;
    }

    public boolean hasTried(int x, int y) {
        validateCoordinates(x, y);
        return shots[y][x] != EMPTY;
    }

    public char getShipCell(int x, int y) {
        validateCoordinates(x, y);
        return ships[y][x];
    }

    public char getShotCell(int x, int y) {
        validateCoordinates(x, y);
        return shots[y][x];
    }

    public int getShipHealth(int shipIndex) {
        if (shipIndex < 0 || shipIndex >= shipHp.length) {
            throw new IllegalArgumentException("Invalid ship index");
        }
        return shipHp[shipIndex];
    }

    private ShotResult receiveShot(int x, int y) {
        validateCoordinates(x, y);
        char current = ships[y][x];
        if (current == HIT) {
            return ShotResult.ALREADY_TRIED;
        }

        if (current == SHIP) {
            ships[y][x] = HIT;
            int shipIndex = shipId[y][x];
            if (shipIndex >= 0 && shipIndex < shipHp.length) {
                shipHp[shipIndex]--;
                if (shipHp[shipIndex] == 0) {
                    return ShotResult.SUNK;
                }
            }
            return ShotResult.HIT;
        }

        ships[y][x] = MISS;
        return ShotResult.MISS;
    }

    private void validateCoordinates(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Coordinates are outside the board");
        }
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    private void fillGrid(char[][] grid, char value) {
        for (char[] row : grid) {
            Arrays.fill(row, value);
        }
    }

    private void fillShipIds(int value) {
        for (int[] row : shipId) {
            Arrays.fill(row, value);
        }
    }

    public int getShipId(int x, int y) {
        validateCoordinates(x, y);
        return shipId[y][x];
    }
}
