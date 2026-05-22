// =========================================================
// player/Player.java
// =========================================================
package player;

import config.GameConfig;
import domain.Board;
import domain.ShotResult;

/**
 * Contrato comum de um jogador.
 * Cada implementação sabe posicionar sua frota e escolher onde atirar.
 */
public abstract class Player {

    protected final String name;
    protected final Board board;        // tabuleiro próprio (navios + tiros recebidos)
    protected final Board shotsBoard;   // registro dos tiros disparados neste jogador

    protected Player(String name, GameConfig config) {
        this.name       = name;
        this.board      = new Board(config);
        this.shotsBoard = new Board(config);
    }

    /** Nome exibido na UI. */
    public String getName() { return name; }

    /** Tabuleiro próprio (navios + acertos recebidos). */
    public Board getBoard() { return board; }

    /**
     * Posiciona a frota no próprio tabuleiro.
     * Implementações humanas podem interagir com o usuário;
     * implementações de CPU posicionam automaticamente.
     */
    public abstract void placeFleet(GameConfig config);

    /**
     * Escolhe e executa um tiro no tabuleiro do adversário.
     * Retorna o resultado do tiro.
     *
     * @param opponent tabuleiro do adversário (Board.ships é o alvo)
     * @param config   configuração da partida (regras de tiro extra, etc.)
     * @return resultado do último tiro disparado neste turno
     */
    public abstract ShotResult takeTurn(Board opponent, GameConfig config);

    /** Navios ainda não afundados. */
    public int remainingShips() { return board.remainingShips(); }

    /** Verdadeiro se toda a frota foi afundada. */
    public boolean isDefeated() { return remainingShips() == 0; }

    public Board getShotsBoard() { return shotsBoard; }
}