package tn.tt.smartops.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " introuvable (id=" + id + ").");
    }
}
