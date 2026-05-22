// =========================================================
// CoordinateTest.java
// =========================================================
import domain.Coordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Coordinate")
class CoordinateTest {

    private static final char COL_START  = 'A';
    private static final char COL_END    = 'J';
    private static final int  BOARD_SIZE = 10;

    // ------------------------------------------------------------------
    // Construção — of()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Coordinate.of()")
    class OfFactory {

        @Test
        @DisplayName("cria com coordenadas válidas")
        void validCoordinates() {
            Coordinate c = Coordinate.of(0, 0);
            assertEquals(0, c.x);
            assertEquals(0, c.y);
        }

        @Test
        @DisplayName("cria no canto máximo")
        void maxCorner() {
            Coordinate c = Coordinate.of(9, 9);
            assertEquals(9, c.x);
            assertEquals(9, c.y);
        }

        @Test
        @DisplayName("x negativo lança exceção")
        void negativeXThrows() {
            assertThrows(IllegalArgumentException.class, () -> Coordinate.of(-1, 0));
        }

        @Test
        @DisplayName("y negativo lança exceção")
        void negativeYThrows() {
            assertThrows(IllegalArgumentException.class, () -> Coordinate.of(0, -1));
        }
    }

    // ------------------------------------------------------------------
    // Parse — válidos
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("parse() — entradas válidas")
    class ParseValid {

        @Test
        @DisplayName("A1 → {0, 0}")
        void parseA1() {
            Coordinate c = Coordinate.parse("A1", COL_START, COL_END, BOARD_SIZE);
            assertNotNull(c);
            assertEquals(0, c.x);
            assertEquals(0, c.y);
        }

        @Test
        @DisplayName("J10 → {9, 9}")
        void parseJ10() {
            Coordinate c = Coordinate.parse("J10", COL_START, COL_END, BOARD_SIZE);
            assertNotNull(c);
            assertEquals(9, c.x);
            assertEquals(9, c.y);
        }

        @Test
        @DisplayName("B7 → {1, 6}")
        void parseB7() {
            Coordinate c = Coordinate.parse("B7", COL_START, COL_END, BOARD_SIZE);
            assertNotNull(c);
            assertEquals(1, c.x);
            assertEquals(6, c.y);
        }

        @Test
        @DisplayName("case-insensitive: a1 = A1")
        void caseInsensitive() {
            Coordinate lower = Coordinate.parse("a1", COL_START, COL_END, BOARD_SIZE);
            Coordinate upper = Coordinate.parse("A1", COL_START, COL_END, BOARD_SIZE);
            assertNotNull(lower);
            assertEquals(upper, lower);
        }

        @Test
        @DisplayName("espaços em torno ignorados")
        void trimmed() {
            Coordinate c = Coordinate.parse("  C3  ", COL_START, COL_END, BOARD_SIZE);
            assertNotNull(c);
            assertEquals(2, c.x);
            assertEquals(2, c.y);
        }
    }

    // ------------------------------------------------------------------
    // Parse — inválidos
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("parse() — entradas inválidas retornam null")
    class ParseInvalid {

        @ParameterizedTest(name = "parse(\"{0}\") == null")
        @ValueSource(strings = {"", "A", "A0", "A11", "K1", "Z9", "1A", "AA1", "!1"})
        @DisplayName("formatos inválidos retornam null")
        void invalidFormatsReturnNull(String input) {
            assertNull(Coordinate.parse(input, COL_START, COL_END, BOARD_SIZE));
        }

        @Test
        @DisplayName("null retorna null")
        void nullInputReturnsNull() {
            assertNull(Coordinate.parse(null, COL_START, COL_END, BOARD_SIZE));
        }

        @Test
        @DisplayName("coluna antes do início retorna null")
        void columnBeforeStartReturnsNull() {
            // '@' é o char antes de 'A'
            assertNull(Coordinate.parse("@1", COL_START, COL_END, BOARD_SIZE));
        }

        @Test
        @DisplayName("linha 0 retorna null")
        void row0ReturnsNull() {
            assertNull(Coordinate.parse("A0", COL_START, COL_END, BOARD_SIZE));
        }

        @Test
        @DisplayName("linha além do tabuleiro retorna null")
        void rowBeyondBoardReturnsNull() {
            assertNull(Coordinate.parse("A11", COL_START, COL_END, BOARD_SIZE));
        }
    }

    // ------------------------------------------------------------------
    // toLabel
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("toLabel()")
    class ToLabel {

        @Test
        @DisplayName("{0,0} → A1")
        void origin() {
            assertEquals("A1", Coordinate.of(0, 0).toLabel(COL_START));
        }

        @Test
        @DisplayName("{9,9} → J10")
        void maxCorner() {
            assertEquals("J10", Coordinate.of(9, 9).toLabel(COL_START));
        }

        @Test
        @DisplayName("parse e toLabel são inversos")
        void roundTrip() {
            String original = "E5";
            Coordinate c = Coordinate.parse(original, COL_START, COL_END, BOARD_SIZE);
            assertNotNull(c);
            assertEquals(original, c.toLabel(COL_START));
        }
    }

    // ------------------------------------------------------------------
    // Vizinhança
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Vizinhos")
    class Neighbors {

        @Test
        @DisplayName("canto (0,0) tem 2 vizinhos ortogonais")
        void cornerHasTwoOrthogonalNeighbors() {
            Coordinate[] n = Coordinate.of(0, 0).orthogonalNeighbors(10);
            assertEquals(2, n.length);
        }

        @Test
        @DisplayName("centro (5,5) tem 4 vizinhos ortogonais")
        void centerHasFourOrthogonalNeighbors() {
            Coordinate[] n = Coordinate.of(5, 5).orthogonalNeighbors(10);
            assertEquals(4, n.length);
        }

        @Test
        @DisplayName("canto (0,0) tem 3 vizinhos diagonais+ortogonais")
        void cornerHasThreeAllNeighbors() {
            Coordinate[] n = Coordinate.of(0, 0).allNeighbors(10);
            assertEquals(3, n.length);
        }

        @Test
        @DisplayName("centro tem 8 vizinhos totais")
        void centerHasEightAllNeighbors() {
            Coordinate[] n = Coordinate.of(5, 5).allNeighbors(10);
            assertEquals(8, n.length);
        }
    }

    // ------------------------------------------------------------------
    // equals / hashCode
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("equals e hashCode")
    class Equality {

        @Test
        @DisplayName("mesmas coordenadas são iguais")
        void sameCoordinatesAreEqual() {
            assertEquals(Coordinate.of(3, 7), Coordinate.of(3, 7));
        }

        @Test
        @DisplayName("coordenadas diferentes não são iguais")
        void differentCoordinatesAreNotEqual() {
            assertNotEquals(Coordinate.of(0, 0), Coordinate.of(0, 1));
        }

        @Test
        @DisplayName("hashCode consistente com equals")
        void hashCodeConsistentWithEquals() {
            Coordinate a = Coordinate.of(4, 4);
            Coordinate b = Coordinate.of(4, 4);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}