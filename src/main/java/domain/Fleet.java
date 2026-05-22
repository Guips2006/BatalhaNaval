// =========================================================
// domain/Fleet.java
// =========================================================
package domain;

import config.GameConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Coleção imutável de navios de uma frota.
 *
 * Fleet é o espelho em objetos do que o monolito mantinha em
 * arrays paralelos: shipLen[], shipName[], shipHp[].
 *
 * Fleet não toca Board diretamente — ela apenas reflete o estado
 * que o Board mantém, sincronizado via {@link #syncFromBoard(Board)}.
 *
 * Sem System.out — puro domínio.
 */
public final class Fleet {

    private final List<Ship> ships;

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------

    private Fleet(Ship[] ships) {
        this.ships = Collections.unmodifiableList(Arrays.asList(ships));
    }

    /**
     * Cria uma frota a partir da configuração do jogo.
     * Nenhum navio está posicionado ainda.
     */
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

    // ------------------------------------------------------------------
    // Sincronização com Board
    // ------------------------------------------------------------------

    /**
     * Atualiza o HP de cada Ship refletindo o estado atual do Board.
     * Deve ser chamado após cada tiro para manter Fleet e Board coerentes.
     *
     * @param board Board do dono desta frota
     */
    public void syncFromBoard(Board board) {
        for (Ship ship : ships) {
            int hp = board.getShipHealth(ship.getIndex());
            ship.syncHp(hp);
        }
    }

    /**
     * Marca todos os navios como posicionados.
     * Chamado por Player após concluir o posicionamento no Board.
     */
    public void markAllPlaced() {
        for (Ship ship : ships) ship.markPlaced();
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /** Lista imutável de todos os navios. */
    public List<Ship> getShips() {
        return ships;
    }

    /** Navio pelo índice (mesmo índice usado pelo Board). */
    public Ship get(int index) {
        return ships.get(index);
    }

    /** Número total de navios na frota. */
    public int size() {
        return ships.size();
    }

    /** Quantidade de navios ainda afloat (hp > 0). */
    public int aliveCount() {
        int count = 0;
        for (Ship s : ships) if (s.isAlive()) count++;
        return count;
    }

    /** Quantidade de navios afundados. */
    public int sunkCount() {
        return ships.size() - aliveCount();
    }

    /** True se todos os navios foram afundados. */
    public boolean isDestroyed() {
        for (Ship s : ships) if (s.isAlive()) return false;
        return true;
    }

    /**
     * Retorna o navio recém-afundado com base no HP antes e depois de um tiro,
     * ou {@code null} se nenhum foi afundado.
     *
     * Útil para o Game saber qual navio afundar e passar a mensagem para a UI.
     *
     * @param board Board atualizado APÓS o tiro
     */
    public Ship detectNewlySunk(Board board) {
        for (Ship ship : ships) {
            boolean wasAlive = ship.isAlive();
            int currentHp   = board.getShipHealth(ship.getIndex());
            if (wasAlive && currentHp == 0) return ship;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Fleet[\n");
        for (Ship s : ships) sb.append("  ").append(s).append('\n');
        return sb.append(']').toString();
    }
}