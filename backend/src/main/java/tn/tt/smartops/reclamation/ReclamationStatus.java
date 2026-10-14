package tn.tt.smartops.reclamation;

import java.util.EnumSet;
import java.util.Set;

/**
 * Suivi côté client. Les transitions AFFECTEE -> EN_COURS -> RESOLUE suivent
 * automatiquement l'intervention créée par le chef d'équipe.
 */
public enum ReclamationStatus {
    NOUVELLE,
    AFFECTEE,
    EN_COURS,
    RESOLUE,
    ANNULEE;

    public static final Set<ReclamationStatus> OPEN = EnumSet.of(NOUVELLE, AFFECTEE, EN_COURS);
}
