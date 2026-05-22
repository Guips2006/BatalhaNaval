// =========================================================
// ShipTest.java
// =========================================================
import domain.Ship;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ship")
class ShipTest {

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Construção")
    class Construction {

        @Test
        @DisplayName("cria navio com estado inicial correto")
        void createsWithCorrectState() {
            Ship s = new Ship(0, "Porta-Aviões", 5);
            assertEquals(0, s.getIndex());
            assertEquals("Porta-Aviões", s.getName());
            assertEquals(5, s.getSize());
            assertEquals(5, s.getHp());
            assertFalse(s.isPlaced());
            assertFalse(s.isSunk());
            assertTrue(s.isAlive());
        }

        @Test
        @DisplayName("tamanho zero ou negativo lança exceção")
        void invalidSizeThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Ship(0, "X", 0));
            assertThrows(IllegalArgumentException.class, () -> new Ship(0, "X", -1));
        }

        @Test
        @DisplayName("nome nulo ou vazio lança exceção")
        void invalidNameThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Ship(0, null, 3));
            assertThrows(IllegalArgumentException.class, () -> new Ship(0, "  ", 3));
        }
    }

    // ------------------------------------------------------------------
    // hit()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("hit()")
    class HitBehavior {

        @Test
        @DisplayName("cada hit decrementa o HP")
        void hitDecrementsHp() {
            Ship s = new Ship(0, "Cruzador", 3);
            s.hit();
            assertEquals(2, s.getHp());
            s.hit();
            assertEquals(1, s.getHp());
        }

        @Test
        @DisplayName("hit até zero afunda o navio")
        void hitToZeroSinks() {
            Ship s = new Ship(0, "Submarino", 2);
            s.hit();
            s.hit();
            assertTrue(s.isSunk());
            assertFalse(s.isAlive());
        }

        @Test
        @DisplayName("HP não vai abaixo de zero")
        void hpDoesNotGoBelowZero() {
            Ship s = new Ship(0, "Barco", 1);
            s.hit();
            s.hit(); // extra hit
            assertEquals(0, s.getHp());
        }
    }

    // ------------------------------------------------------------------
    // syncHp()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("syncHp()")
    class SyncHp {

        @Test
        @DisplayName("sincroniza HP com valor válido")
        void syncsValidHp() {
            Ship s = new Ship(0, "Fragata", 4);
            s.syncHp(2);
            assertEquals(2, s.getHp());
        }

        @Test
        @DisplayName("syncHp(0) marca como afundado")
        void syncZeroSinks() {
            Ship s = new Ship(0, "Fragata", 4);
            s.syncHp(0);
            assertTrue(s.isSunk());
        }

        @Test
        @DisplayName("HP acima do tamanho lança exceção")
        void hpAboveSizeThrows() {
            Ship s = new Ship(0, "Fragata", 4);
            assertThrows(IllegalArgumentException.class, () -> s.syncHp(5));
        }

        @Test
        @DisplayName("HP negativo lança exceção")
        void negativeHpThrows() {
            Ship s = new Ship(0, "Fragata", 4);
            assertThrows(IllegalArgumentException.class, () -> s.syncHp(-1));
        }
    }

    // ------------------------------------------------------------------
    // markPlaced / integrityRatio
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Estado e integridade")
    class StateAndIntegrity {

        @Test
        @DisplayName("markPlaced altera isPlaced")
        void markPlacedWorks() {
            Ship s = new Ship(0, "Destroyer", 3);
            assertFalse(s.isPlaced());
            s.markPlaced();
            assertTrue(s.isPlaced());
        }

        @Test
        @DisplayName("integrityRatio 1.0 quando intacto")
        void integrityRatioIntact() {
            Ship s = new Ship(0, "Destroyer", 4);
            assertEquals(1.0, s.integrityRatio(), 0.001);
        }

        @Test
        @DisplayName("integrityRatio 0.0 quando afundado")
        void integrityRatioSunk() {
            Ship s = new Ship(0, "Destroyer", 2);
            s.hit(); s.hit();
            assertEquals(0.0, s.integrityRatio(), 0.001);
        }

        @Test
        @DisplayName("integrityRatio proporcional ao HP restante")
        void integrityRatioPartial() {
            Ship s = new Ship(0, "Destroyer", 4);
            s.hit(); s.hit();
            assertEquals(0.5, s.integrityRatio(), 0.001);
        }
    }
}