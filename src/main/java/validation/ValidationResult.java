// =========================================================
// validation/ValidationResult.java
// =========================================================
package validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado imutável de uma validação de frota.
 * Acumula zero ou mais mensagens de erro; se não houver nenhum, a frota é válida.
 */
public final class ValidationResult {

    private final List<String> errors;

    private ValidationResult(List<String> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /** Cria um resultado de sucesso (sem erros). */
    public static ValidationResult ok() {
        return new ValidationResult(Collections.emptyList());
    }

    /** Cria um resultado com um único erro. */
    public static ValidationResult fail(String message) {
        return new ValidationResult(List.of(message));
    }

    /** Cria um resultado a partir de uma lista de mensagens (pode ser vazia = sucesso). */
    public static ValidationResult of(List<String> errors) {
        return new ValidationResult(errors);
    }

    /** {@code true} se não houver erros. */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /** Lista imutável de mensagens de erro (vazia se válido). */
    public List<String> getErrors() {
        return errors;
    }

    /** Primeira mensagem de erro, ou string vazia se válido. */
    public String firstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }

    /** Todas as mensagens separadas por nova linha. */
    public String summary() {
        return String.join(System.lineSeparator(), errors);
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult{OK}" : "ValidationResult{errors=" + errors + "}";
    }
}