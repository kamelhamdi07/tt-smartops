package tn.tt.smartops.intervention;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Machine à états du cycle de vie d'une intervention.
 * NOUVELLE -> PLANIFIEE -> AFFECTEE -> EN_COURS <-> SUSPENDUE -> TERMINEE -> VALIDEE (clôture)
 * L'annulation est possible tant que l'intervention n'est pas terminée.
 */
public enum InterventionStatus {
    NOUVELLE,
    PLANIFIEE,
    AFFECTEE,
    EN_COURS,
    SUSPENDUE,
    TERMINEE,
    VALIDEE,
    ANNULEE;

    private static final Map<InterventionStatus, Set<InterventionStatus>> TRANSITIONS = Map.of(
            NOUVELLE, EnumSet.of(PLANIFIEE, ANNULEE),
            PLANIFIEE, EnumSet.of(AFFECTEE, ANNULEE),
            AFFECTEE, EnumSet.of(PLANIFIEE, EN_COURS, ANNULEE),
            EN_COURS, EnumSet.of(SUSPENDUE, TERMINEE),
            SUSPENDUE, EnumSet.of(EN_COURS, ANNULEE),
            TERMINEE, EnumSet.of(VALIDEE),
            VALIDEE, EnumSet.noneOf(InterventionStatus.class),
            ANNULEE, EnumSet.noneOf(InterventionStatus.class));

    /** Statuts pour lesquels l'intervention occupe encore ses ressources. */
    public static final Set<InterventionStatus> BLOCKING = EnumSet.of(PLANIFIEE, AFFECTEE, EN_COURS, SUSPENDUE);

    /** Statuts dans lesquels la planification (dates, ressources, équipe) reste modifiable. */
    public static final Set<InterventionStatus> EDITABLE = EnumSet.of(NOUVELLE, PLANIFIEE, AFFECTEE);

    public boolean canTransitionTo(InterventionStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }
}
