// =========================================================
// BoardTest.java
// =========================================================
import config.AdjacencyRule;
import config.CpuStrategy;
import config.GameMode;
import domain.Board;
import domain.ShotResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Board")
class BoardTest {

    // frota mínima: 1 navio de tamanho 3, 1 de tamanho 2
    private static final int[] FLEET = {3, 2};
    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(10, FLEET);
    }

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Construção")
    class Construction {

        @Test
        @DisplayName("tamanho inválido lança exceção")
        void invalidSizeThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Board(0, FLEET));
            assertThrows(IllegalArgumentException.class, () -> new Board(-1, FLEET));
        }

        @Test
        @DisplayName("frota nula ou vazia lança exceção")
        void nullFleetThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Board(10, null));
            assertThrows(IllegalArgumentException.class, () -> new Board(10, new int[]{}));
        }

        @Test
        @DisplayName("tabuleiro começa todo vazio")
        void initiallyEmpty() {
            for (int y = 0; y < board.getSize(); y++)
                for (int x = 0; x < board.getSize(); x++) {
                    assertEquals(Board.EMPTY, board.getShipCell(x, y));
                    assertEquals(Board.EMPTY, board.getShotCell(x, y));
                }
        }

        @Test
        @DisplayName("getSize retorna o tamanho configurado")
        void sizeIsCorrect() {
            assertEquals(10, board.getSize());
        }
    }

    // ------------------------------------------------------------------
    // Posicionamento
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Posicionamento de navios")
    class Placement {

        @Test
        @DisplayName("posiciona navio horizontal dentro do limite")
        void placeHorizontal() {
            board.placeShip(0, 0, 3, true, 0);
            assertEquals(Board.SHIP, board.getShipCell(0, 0));
            assertEquals(Board.SHIP, board.getShipCell(1, 0));
            assertEquals(Board.SHIP, board.getShipCell(2, 0));
            assertEquals(Board.EMPTY, board.getShipCell(3, 0));
        }

        @Test
        @DisplayName("posiciona navio vertical dentro do limite")
        void placeVertical() {
            board.placeShip(5, 2, 3, false, 0);
            assertEquals(Board.SHIP, board.getShipCell(5, 2));
            assertEquals(Board.SHIP, board.getShipCell(5, 3));
            assertEquals(Board.SHIP, board.getShipCell(5, 4));
            assertEquals(Board.EMPTY, board.getShipCell(5, 5));
        }

        @Test
        @DisplayName("canPlaceShip falso quando extrapola limite horizontal")
        void cannotPlaceBeyondEdgeHorizontal() {
            assertFalse(board.canPlaceShip(8, 0, 3, true));  // 8+3=11 > 10
        }

        @Test
        @DisplayName("canPlaceShip falso quando extrapola limite vertical")
        void cannotPlaceBeyondEdgeVertical() {
            assertFalse(board.canPlaceShip(0, 9, 3, false)); // 9+3=12 > 10
        }

        @Test
        @DisplayName("canPlaceShip falso quando há colisão")
        void cannotPlaceOverlapping() {
            board.placeShip(0, 0, 3, true, 0);
            assertFalse(board.canPlaceShip(1, 0, 2, true));  // célula (1,0) ocupada
        }

        @Test
        @DisplayName("placeShip lança exceção se posição inválida")
        void placeShipThrowsOnInvalidPosition() {
            assertThrows(IllegalArgumentException.class,
                () -> board.placeShip(8, 0, 3, true, 0));    // extrapola
        }

        @Test
        @DisplayName("placeShip lança exceção para índice de navio inválido")
        void placeShipThrowsOnBadIndex() {
            assertThrows(IllegalArgumentException.class,
                () -> board.placeShip(0, 0, 3, true, 99));   // índice fora da frota
        }

        @Test
        @DisplayName("HP inicial do navio = tamanho configurado")
        void initialHpEqualsSize() {
            board.placeShip(0, 0, 3, true, 0);
            assertEquals(3, board.getShipHealth(0));
        }

        @Test
        @DisplayName("reset limpa navios e tiros")
        void resetClearsBoard() {
            board.placeShip(0, 0, 3, true, 0);
            board.reset();
            assertEquals(Board.EMPTY, board.getShipCell(0, 0));
        }
    }

    // ------------------------------------------------------------------
    // Tiros
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Aplicação de tiros")
    class Shots {

        private Board attacker;
        private Board defender;

        @BeforeEach
        void setUpBoards() {
            attacker = new Board(10, FLEET);
            defender = new Board(10, FLEET);
            defender.placeShip(0, 0, 3, true, 0);  // navio 0: (0,0),(1,0),(2,0)
            defender.placeShip(5, 5, 2, false, 1); // navio 1: (5,5),(5,6)
        }

        @Test
        @DisplayName("tiro na água retorna MISS")
        void missReturnsCorrectResult() {
            ShotResult r = attacker.fireAt(defender, 9, 9);
            assertEquals(ShotResult.MISS, r);
        }

        @Test
        @DisplayName("célula de tiro marcada após MISS")
        void missCellMarked() {
            attacker.fireAt(defender, 9, 9);
            assertTrue(attacker.hasTried(9, 9));
        }

        @Test
        @DisplayName("tiro em navio retorna HIT")
        void hitReturnsCorrectResult() {
            ShotResult r = attacker.fireAt(defender, 0, 0);
            assertEquals(ShotResult.HIT, r);
        }

        @Test
        @DisplayName("acerto decrementa HP do navio")
        void hitDecrementsHp() {
            attacker.fireAt(defender, 0, 0);
            assertEquals(2, defender.getShipHealth(0));
        }

        @Test
        @DisplayName("afundar navio retorna SUNK")
        void sunkReturnsCorrectResult() {
            attacker.fireAt(defender, 5, 5);
            ShotResult r = attacker.fireAt(defender, 5, 6);
            assertEquals(ShotResult.SUNK, r);
        }

        @Test
        @DisplayName("navio afundado tem HP zero")
        void sunkShipHpIsZero() {
            attacker.fireAt(defender, 5, 5);
            attacker.fireAt(defender, 5, 6);
            assertEquals(0, defender.getShipHealth(1));
        }

        @Test
        @DisplayName("tiro repetido retorna ALREADY_TRIED")
        void repeatedShotReturnsAlreadyTried() {
            attacker.fireAt(defender, 0, 0);
            ShotResult r = attacker.fireAt(defender, 0, 0);
            assertEquals(ShotResult.ALREADY_TRIED, r);
        }

        @Test
        @DisplayName("hasTried falso antes de atirar")
        void hasTriedFalseBeforeShot() {
            assertFalse(attacker.hasTried(3, 3));
        }

        @Test
        @DisplayName("hasTried verdadeiro após atirar")
        void hasTriedTrueAfterShot() {
            attacker.fireAt(defender, 3, 3);
            assertTrue(attacker.hasTried(3, 3));
        }

        @Test
        @DisplayName("fireAt com oponente nulo lança exceção")
        void fireAtNullOpponentThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> attacker.fireAt(null, 0, 0));
        }
    }

    // ------------------------------------------------------------------
    // Fim de jogo
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Fim de jogo")
    class EndGame {

        @Test
        @DisplayName("remainingShips correto antes de qualquer tiro")
        void remainingShipsInitial() {
            board.placeShip(0, 0, 3, true, 0);
            board.placeShip(0, 2, 2, true, 1);
            assertEquals(2, board.remainingShips());
        }

        @Test
        @DisplayName("frota termina quando todos os navios são afundados")
        void fleetDestroyedWhenAllSunk() {
            Board attacker = new Board(10, FLEET);
            Board defender = new Board(10, FLEET);
            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(0, 2, 2, true, 1);

            // Afunda navio 0
            attacker.fireAt(defender, 0, 0);
            attacker.fireAt(defender, 1, 0);
            attacker.fireAt(defender, 2, 0);
            // Afunda navio 1
            attacker.fireAt(defender, 0, 2);
            attacker.fireAt(defender, 1, 2);

            assertEquals(0, defender.remainingShips());
        }

        @Test
        @DisplayName("remainingShips decrementa a cada navio afundado")
        void remainingShipsDecrementsOnSunk() {
            Board attacker = new Board(10, FLEET);
            Board defender = new Board(10, FLEET);
            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(0, 2, 2, true, 1);

            attacker.fireAt(defender, 0, 0);
            attacker.fireAt(defender, 1, 0);
            attacker.fireAt(defender, 2, 0); // afunda navio 0

            assertEquals(1, defender.remainingShips());
        }
    }

    // ------------------------------------------------------------------
    // Coordenadas fora do limite
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Limites de coordenadas")
    class BoundsChecking {

        @Test
        @DisplayName("getShipCell fora do limite lança exceção")
        void getShipCellOutOfBoundsThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> board.getShipCell(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> board.getShipCell(0, 10));
        }

        @Test
        @DisplayName("hasTried fora do limite lança exceção")
        void hasTriedOutOfBoundsThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> board.hasTried(10, 0));
        }

        @Test
        @DisplayName("getShipHealth índice inválido lança exceção")
        void getShipHealthBadIndexThrows() {
            assertThrows(IllegalArgumentException.class, () -> board.getShipHealth(-1));
            assertThrows(IllegalArgumentException.class, () -> board.getShipHealth(99));
        }
    }
}