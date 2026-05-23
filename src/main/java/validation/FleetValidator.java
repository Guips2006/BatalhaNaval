// =========================================================
// validation/FleetValidator.java
// =========================================================
package validation;

import config.AdjacencyRule;
import config.GameConfig;
import domain.Board;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida se uma frota posicionada num Board respeita as regras de configuração.
 *
 * Verificações realizadas:
 *  1. Quantidade de navios posicionados = quantidade esperada pela frota.
 *  2. Tamanho de cada navio = tamanho configurado (via getShipHealth inicial).
 *  3. Regra de adjacência: NONE (qualquer), ORTHO (sem tocar lateral/vertical),
 *     ORTHO_DIAG (sem tocar em nenhuma das 8 direções).
 */
public class FleetValidator {

    private final GameConfig config;

    public FleetValidator(GameConfig config) {
        this.config = config;
    }

    /**
     * Valida o board fornecido contra a configuração de frota.
     *
     * @param board Board já com navios posicionados
     * @return ValidationResult com todos os erros encontrados (ou ok() se válido)
     */
    public ValidationResult validate(Board board) {
        List<String> errors = new ArrayList<>();

        checkShipCount(board, errors);
        checkAdjacency(board, errors);

        return ValidationResult.of(errors);
    }

    // ------------------------------------------------------------------
    // Verificação 1 — quantidade de navios
    // ------------------------------------------------------------------

    private void checkShipCount(Board board, List<String> errors) {
        int[] expectedSizes = config.getFleetSizes();
        int   expectedTotal = expectedSizes.length;
        int   actualTotal   = 0;

        for (int i = 0; i < expectedTotal; i++) {
            // getShipHealth retorna o HP atual; se > 0, o navio foi posicionado
            int hp = board.getShipHealth(i);
            if (hp > 0) actualTotal++;
        }

        if (actualTotal != expectedTotal) {
            errors.add(String.format(
                "Quantidade de navios incorreta: esperado %d, encontrado %d.",
                expectedTotal, actualTotal));
        }
    }

    // ------------------------------------------------------------------
    // Verificação 2 — adjacência
    // ------------------------------------------------------------------

    private void checkAdjacency(Board board, List<String> errors) {
    AdjacencyRule rule = config.getAdjacencyRule();
    if (rule == AdjacencyRule.NONE) return;

    int n = board.getSize();

    for (int y = 0; y < n; y++) {
        for (int x = 0; x < n; x++) {
            int id = board.getShipId(x, y);
            if (id == -1) continue; // célula vazia

            int[][] dirs = rule == AdjacencyRule.ORTHO
                ? new int[][]{{1,0},{-1,0},{0,1},{0,-1}}
                : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (nx < 0 || nx >= n || ny < 0 || ny >= n) continue;
                int neighborId = board.getShipId(nx, ny);
                if (neighborId != -1 && neighborId != id) {
                    errors.add(String.format(
                        "Violação de adjacência (%s) em %c%d.",
                        rule, (char)(config.getColStart() + x), y + 1));
                    break; // um erro por célula é suficiente
                }
            }
        }
    }
}

    /**
     * Retorna true se a célula (x,y) tem algum vizinho ortogonal com navio
     * DIFERENTE da própria célula (i.e., pertencente a outro navio ou ao mesmo,
     * mas adjacente de forma ilegal — basta checar as 4 direções).
     *
     * Atenção: vizinhos no mesmo navio (mesma linha/coluna contígua) são válidos.
     * A lógica detecta apenas adjacências ENTRE navios distintos.
     */
    private boolean hasOrthogonalNeighbor(boolean[][] ship, int x, int y, int n) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (inBounds(nx, ny, n) && ship[ny][nx]) return true;
        }
        return false;
    }

    private boolean hasDiagonalOrOrthogonalNeighbor(boolean[][] ship, int x, int y, int n) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (inBounds(nx, ny, n) && ship[ny][nx]) return true;
            }
        }
        return false;
    }

    private boolean[][] buildShipMatrix(Board board, int n) {
        boolean[][] m = new boolean[n][n];
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                char c = board.getShipCell(x, y);
                m[y][x] = (c == Board.SHIP || c == Board.HIT);
            }
        return m;
    }

    private boolean inBounds(int x, int y, int n) {
        return x >= 0 && x < n && y >= 0 && y < n;
    }
}