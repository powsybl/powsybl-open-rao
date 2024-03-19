package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertNetworkActionImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemedialActionCreationTest {
    /*
    - disabled RA
    - test all combinations of TSO and name
    - test usage rules
    - speed
    - elementary actions
    - preventive with contingency
    - network action with one elementary action of each type
     */
    @Test
    void testImportRemedialActionWithMultipleContingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_MultipleContingenciesForTheSameRemedialAction.zip", NETWORK);

        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "ra2", Set.of("BBE1AA1  BBE4AA1  1"), false, 2);
        assertRaNotImported(cracCreationContext, "ra1", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action ra1 will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency co1 are different");
    }

    @Test
    void testIgnoreInvalidTopologicalActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-9/CSA_9_5_InvalidProfiles.zip", NETWORK);

        assertEquals(0, cracCreationContext.getCrac().getRemedialActions().size());

        assertRaNotImported(cracCreationContext, "unavailable-topological-remedial-action", ImportStatus.NOT_FOR_RAO, "Remedial action unavailable-topological-remedial-action will not be imported because RemedialAction.normalAvailable must be 'true' to be imported");
        assertRaNotImported(cracCreationContext, "undefined-topological-remedial-action", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action undefined-topological-remedial-action will not be imported because there is no elementary action for that RA");
        assertRaNotImported(cracCreationContext, "topological-remedial-action-with-not-existing-switch", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action topological-remedial-action-with-not-existing-switch will not be imported because network model does not contain a switch with id: unknown-switch");
        assertRaNotImported(cracCreationContext, "topological-remedial-action-with-wrong-property-reference", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action 'topological-remedial-action-with-wrong-property-reference' will not be imported because 'TopologyAction' must have a property reference with 'http://energy.referencedata.eu/PropertyReference/Switch.open' value, but it was: 'http://energy.referencedata.eu/PropertyReference/RotatingMachine.p'");
        assertRaNotImported(cracCreationContext, "preventive-topological-remedial-action-with-contingency", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action preventive-topological-remedial-action-with-contingency will not be imported because it is linked to a contingency but it's kind is not curative");
    }
}
