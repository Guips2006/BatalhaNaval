// =========================================================
// domain/Ship.java
// =========================================================
package domain;

/**
 * Representa um navio individual: tamanho, nome, posição e HP.
 *
 * Ship não conhece Board — ele guarda só seu próprio estado.
 * O Board continua sendo a fonte de verdade para posicionamento;
 * Ship é o objeto de domínio que complementa os arrays paralelos
 * que existiam no monolito.
 *
 * Sem System.out — puro domínio.
 */
public final class Ship {

    private final int    index;   // posição na frota (0-based), igual ao shipId do Board
    private final String name;
    private final int    size;
    private       int    hp;      // decrementado por Board.receiveShot via getShipHealth
    private       boolean placed;

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------

    public Ship(int index, String name, int size) {
        if (size <= 0) throw new IllegalArgumentException("Tamanho do navio deve ser positivo.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio.");
        this.index  = index;
        this.name   = name;
        this.size   = size;
        this.hp     = size;   // começa com HP cheio
        this.placed = false;
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    /** Registra um acerto. HP nunca vai abaixo de 0. */
    public void hit() {
        if (hp > 0) hp--;
    }

    /** Sincroniza o HP deste objeto com o valor lido do Board (pós-tiro). */
    public void syncHp(int currentHp) {
        if (currentHp < 0 || currentHp > size) {
            throw new IllegalArgumentException("HP inválido: " + currentHp);
        }
        this.hp = currentHp;
    }

    /** Marca que o navio foi posicionado no tabuleiro. */
    public void markPlaced() {
        this.placed = true;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    public int    getIndex()    { return index;  }
    public String getName()     { return name;   }
    public int    getSize()     { return size;   }
    public int    getHp()       { return hp;     }
    public boolean isPlaced()   { return placed; }
    public boolean isSunk()     { return hp == 0; }
    public boolean isAlive()    { return hp > 0;  }

    /**
     * Percentual de integridade (0.0 afundado … 1.0 intacto).
     * Útil para UI de status.
     */
    public double integrityRatio() {
        return (double) hp / size;
    }

    // ------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("Ship{#%d '%s' size=%d hp=%d placed=%s}",
                index, name, size, hp, placed);
    }
}