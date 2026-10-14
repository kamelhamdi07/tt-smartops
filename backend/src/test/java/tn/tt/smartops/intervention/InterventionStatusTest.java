package tn.tt.smartops.intervention;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static tn.tt.smartops.intervention.InterventionStatus.*;

class InterventionStatusTest {

    @Test
    void mainWorkflowIsAllowed() {
        assertTrue(NOUVELLE.canTransitionTo(PLANIFIEE));
        assertTrue(PLANIFIEE.canTransitionTo(AFFECTEE));
        assertTrue(AFFECTEE.canTransitionTo(EN_COURS));
        assertTrue(EN_COURS.canTransitionTo(TERMINEE));
        assertTrue(TERMINEE.canTransitionTo(VALIDEE));
    }

    @Test
    void cannotSkipSteps() {
        assertFalse(NOUVELLE.canTransitionTo(EN_COURS));
        assertFalse(PLANIFIEE.canTransitionTo(TERMINEE));
        assertFalse(EN_COURS.canTransitionTo(VALIDEE));
    }

    @Test
    void finishedInterventionsAreFinal() {
        for (InterventionStatus s : values()) {
            assertFalse(VALIDEE.canTransitionTo(s));
            assertFalse(ANNULEE.canTransitionTo(s));
        }
        assertFalse(TERMINEE.canTransitionTo(ANNULEE));
    }
}
