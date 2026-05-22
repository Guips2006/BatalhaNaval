package domain;

import config.GameConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Fleet {

    private final List<Ship> ships;

    private Fleet(Ship[] ships) {
        this.ships = Collections.unmodifiableList(Arrays.asList(ships));
    }

    public static Fleet fromConfig(GameConfig config) {
        int[]    sizes = config.getFleetSizes();
        String[] names = config.getFleetNames();

        if (sizes.length != names.length) {
            throw new IllegalArgumentException(
                "fleet.sizes e fleet.names têm comprimentos diferentes.");
        }

        Ship[] ships = new Ship[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            ships[i] = new Ship(i, names[i].trim(), sizes[i]);
        }
        return new Fleet(ships);
    }

    public void syncFromBoard(Board board) {
        for (Ship ship : ships) {
            int hp = board.getShipHealth(ship.getIndex());
            ship.syncHp(hp);
        }
    }

    public void markAllPlaced() {
        for (Ship ship : ships) ship.markPlaced();
    }

    public List<Ship> getShips() {
        return ships;
    }

    public Ship get(int index) {
        return ships.get(index);
    }

    public int size() {
        return ships.size();
    }

    public int aliveCount() {
        int count = 0;
        for (Ship s : ships) if (s.isAlive()) count++;
        return count;
    }

    public int sunkCount() {
        return ships.size() - aliveCount();
    }

    public boolean isDestroyed() {
        for (Ship s : ships) if (s.isAlive()) return false;
        return true;
    }

    public Ship detectNewlySunk(Board board) {
        for (Ship ship : ships) {
            boolean wasAlive = ship.isAlive();
            int currentHp   = board.getShipHealth(ship.getIndex());
            if (wasAlive && currentHp == 0) return ship;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Fleet[\n");
        for (Ship s : ships) sb.append("  ").append(s).append('\n');
        return sb.append(']').toString();
    }
}
