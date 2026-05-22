// =========================================================
// GameTest.java
// =========================================================
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import config.AdjacencyRule;
import domain.Board;
import domain.ShotResult;

/**
 * Testa o motor do jogo sem Scanner, sem prints e sem banco.
 *
 * Em vez de instanciar Game (que precisa de Scanner), testamos
 * diretamente Board + Fleet + as regras de fim de jogo —
 * que é onde o comportamento do motor realmente vive.
 *
 * Quando Game expuser um construtor sem Scanner (para testes de integração)
 * os testes @Nested "Orquestração" podem ser expandidos.
 */
@DisplayName("Motor do jogo")
class GameTest {

    private static final int[]    SIZES = {3, 2};
    private static final String[] NAMES = {"Cruzador", "Barco"};

    // ------------------------------------------------------------------
    // Ciclo completo de tiro → afundamento → fim de jogo
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Sequência de tiros e fim de jogo")
    class ShotSequence {

        @Test
        @DisplayName("jogo termina quando todos os navios do defensor são afundados")
        void gameEndsWhenAllShipsSunk() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);

            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(0, 2, 2, true, 1);

            // Afunda navio 0
            attacker.fireAt(defender, 0, 0);
            attacker.fireAt(defender, 1, 0);
            attacker.fireAt(defender, 2, 0);

            assertEquals(1, defender.remainingShips());

            // Afunda navio 1
            attacker.fireAt(defender, 0, 2);
            attacker.fireAt(defender, 1, 2);

            assertEquals(0, defender.remainingShips());
        }

        @Test
        @DisplayName("condição de vitória: remainingShips == 0")
        void victoryCondition() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(0, 2, 2, true, 1);

            // Dispara em todas as células dos navios
            for (int x = 0; x < 3; x++) attacker.fireAt(defender, x, 0);
            for (int x = 0; x < 2; x++) attacker.fireAt(defender, x, 2);

            assertTrue(defender.remainingShips() == 0,
                "Frota deve ter 0 navios restantes para vitória");
        }

        @Test
        @DisplayName("jogo não termina enquanto houver navios restantes")
        void gameNotOverWhileShipsRemain() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(0, 2, 2, true, 1);

            // Afunda apenas o primeiro navio
            attacker.fireAt(defender, 0, 0);
            attacker.fireAt(defender, 1, 0);
            attacker.fireAt(defender, 2, 0);

            assertTrue(defender.remainingShips() > 0,
                "Deve haver navios restantes após afundar apenas um");
        }
    }

    // ------------------------------------------------------------------
    // Regra: tiro já tentado não altera o estado
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Tiro já tentado")
    class AlreadyTried {

        @Test
        @DisplayName("tiro repetido retorna ALREADY_TRIED e não altera HP")
        void repeatedShotDoesNotAlterHp() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);

            attacker.fireAt(defender, 0, 0);  // HIT — HP: 3→2
            int hpAfterFirst = defender.getShipHealth(0);

            ShotResult repeat = attacker.fireAt(defender, 0, 0);
            assertEquals(ShotResult.ALREADY_TRIED, repeat);
            assertEquals(hpAfterFirst, defender.getShipHealth(0),
                "HP não deve mudar em tiro repetido");
        }
    }

    // ------------------------------------------------------------------
    // Regra: tiro só acerta navio no tabuleiro do defensor
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Isolamento de tabuleiros")
    class BoardIsolation {

        @Test
        @DisplayName("tiro do atacante não altera seu próprio tabuleiro de navios")
        void attackerShipBoardUnchanged() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);
            attacker.placeShip(5, 5, 2, true, 1);

            attacker.fireAt(defender, 0, 0);

            // O tabuleiro de navios do atacante não muda
            assertEquals(Board.SHIP, attacker.getShipCell(5, 5));
        }

        @Test
        @DisplayName("tiro registrado no board de tiros do atacante, não no defensor")
        void shotRegisteredOnAttackerShotBoard() {
            Board attacker = new Board(10, SIZES);
            Board defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);

            attacker.fireAt(defender, 0, 0);

            assertTrue(attacker.hasTried(0, 0));
            // Defensor não registra "tentativa" — só recebe dano na grade de navios
            assertEquals(Board.HIT, defender.getShipCell(0, 0));
        }
    }

    // ------------------------------------------------------------------
    // Resultados de tiro
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Resultados possíveis de tiro")
    class ShotResults {

        private Board attacker;
        private Board defender;

        {
            attacker = new Board(10, SIZES);
            defender = new Board(10, SIZES);
            defender.placeShip(0, 0, 3, true, 0);
            defender.placeShip(5, 5, 2, false, 1);
        }

        @Test
        @DisplayName("MISS em célula vazia")
        void missOnEmpty() {
            assertEquals(ShotResult.MISS, attacker.fireAt(defender, 9, 9));
        }

        @Test
        @DisplayName("HIT em célula de navio (sem afundar)")
        void hitWithoutSinking() {
            assertEquals(ShotResult.HIT, attacker.fireAt(defender, 0, 0));
        }

        @Test
        @DisplayName("SUNK ao acertar a última célula do navio")
        void sunkOnLastCell() {
            attacker.fireAt(defender, 5, 5);
            assertEquals(ShotResult.SUNK, attacker.fireAt(defender, 5, 6));
        }

        @Test
        @DisplayName("ALREADY_TRIED em célula repetida")
        void alreadyTriedOnRepeat() {
            attacker.fireAt(defender, 3, 3);
            assertEquals(ShotResult.ALREADY_TRIED, attacker.fireAt(defender, 3, 3));
        }
    }

    // ------------------------------------------------------------------
    // Fleet integrado com Board
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Fleet sincronizado com Board")
    class FleetBoardIntegration {

        @Test
        @DisplayName("syncFromBoard atualiza HP dos navios")
        void syncUpdatesShipHp() {
            FleetValidatorTest.StubConfig cfg =
                new FleetValidatorTest.StubConfig(10, 'A', 'J', SIZES, NAMES, AdjacencyRule.NONE);

            Board board = new Board(10, SIZES);
            board.placeShip(0, 0, 3, true, 0);
            board.placeShip(0, 2, 2, true, 1);

            domain.Fleet fleet = domain.Fleet.fromConfig(cfg);

            // Antes de qualquer tiro
            fleet.syncFromBoard(board);
            assertEquals(2, fleet.aliveCount());

            // Simula tiro que afunda navio 1 (size=2)
            Board attacker = new Board(10, SIZES);
            attacker.fireAt(board, 0, 2);
            attacker.fireAt(board, 1, 2);

            fleet.syncFromBoard(board);
            assertEquals(1, fleet.aliveCount());
            assertTrue(fleet.get(1).isSunk());
        }

        @Test
        @DisplayName("isDestroyed quando todos afundados")
        void fleetIsDestroyedWhenAllSunk() {
            FleetValidatorTest.StubConfig cfg =
                new FleetValidatorTest.StubConfig(10, 'A', 'J', SIZES, NAMES, AdjacencyRule.NONE);

            Board board    = new Board(10, SIZES);
            Board attacker = new Board(10, SIZES);
            board.placeShip(0, 0, 3, true, 0);
            board.placeShip(0, 2, 2, true, 1);

            domain.Fleet fleet = domain.Fleet.fromConfig(cfg);

            for (int x = 0; x < 3; x++) attacker.fireAt(board, x, 0);
            for (int x = 0; x < 2; x++) attacker.fireAt(board, x, 2);

            fleet.syncFromBoard(board);
            assertTrue(fleet.isDestroyed());
        }
    }
}