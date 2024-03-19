/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class TopologicalActionCreationTest {

    // TODO: remove CGMES from achives + rename + gather all in one test

    @Test
    void testImportNetworkActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/csa-9/CSA_9_4_ValidProfiles.zip", NETWORK);
        Instant preventiveInstant = cracCreationContext.getCrac().getInstant("preventive");
        Instant curativeInstant = cracCreationContext.getCrac().getInstant("curative");

        assertEquals(7, cracCreationContext.getCrac().getRemedialActions().size());

        // RA1 (on instant)
        assertTopologicalActionImported(cracCreationContext, "on-instant-preventive-topological-remedial-action", "RA1", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "on-instant-preventive-topological-remedial-action", preventiveInstant, UsageMethod.AVAILABLE);

        // RA2 (on instant)
        assertTopologicalActionImported(cracCreationContext, "on-instant-curative-topological-remedial-action", "RA2", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "on-instant-curative-topological-remedial-action", curativeInstant, UsageMethod.AVAILABLE);

        // RA3 (on state)
        assertTopologicalActionImported(cracCreationContext, "on-contingency-state-considered-curative-topological-remedial-action", "RA3", "BBE1AA1  BBE4AA1  1");
        assertHasOnContingencyStateUsageRule(cracCreationContext, "on-contingency-state-considered-curative-topological-remedial-action", "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7", curativeInstant, UsageMethod.AVAILABLE);

        // RA4 (on state)
        assertTopologicalActionImported(cracCreationContext, "on-contingency-state-included-curative-topological-remedial-action", "RA4", "BBE1AA1  BBE4AA1  1");
        assertHasOnContingencyStateUsageRule(cracCreationContext, "on-contingency-state-included-curative-topological-remedial-action", "bbda9fe0-77e0-4f8e-b9d9-4402a539f2b7", curativeInstant, UsageMethod.AVAILABLE);

        // nameless-topological-remedial-action-with-speed (on instant)
        assertTopologicalActionImported(cracCreationContext, "nameless-topological-remedial-action-with-speed", "nameless-topological-remedial-action-with-speed", "BBE1AA1  BBE4AA1  1", 137);

        // RTE_RA6 (on instant)
        assertTopologicalActionImported(cracCreationContext, "topological-remedial-action-with-tso-name", "RTE_RA6", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "topological-remedial-action-with-tso-name", preventiveInstant, UsageMethod.AVAILABLE);

        // nameless-topological-remedial-action-with-tso-name-parent (on instant)
        assertTopologicalActionImported(cracCreationContext, "nameless-topological-remedial-action-with-tso-name-parent", "nameless-topological-remedial-action-with-tso-name-parent", "BBE1AA1  BBE4AA1  1");
        assertHasOnInstantUsageRule(cracCreationContext, "nameless-topological-remedial-action-with-tso-name-parent", preventiveInstant, UsageMethod.AVAILABLE);
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

    @Test
    void testImportRemedialActionWithMultipleContingencies() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_MultipleContingenciesForTheSameRemedialAction.zip", NETWORK);

        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "ra2", Set.of("BBE1AA1  BBE4AA1  1"), false, 2);
        assertRaNotImported(cracCreationContext, "ra1", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action ra1 will not be imported because the ElementCombinationConstraintKinds that link the remedial action to the contingency co1 are different");
    }

    @Test
    void testTopologicalActionOpenClose() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_74_TopoOpenClose.zip", NETWORK);
        assertNotNull(cracCreationContext);
        assertEquals(1, cracCreationContext.getCrac().getRemedialActions().size());
        assertNetworkActionImported(cracCreationContext, "topology-action", Set.of("BBE1AA1  BBE4AA1  1", "DDE3AA1  DDE4AA1  1"), false, 1);
        cracCreationContext.getCrac().getNetworkAction("topology-action").getElementaryActions();
        Iterator<?> it = cracCreationContext.getCrac().getNetworkAction("topology-action").getElementaryActions().iterator();
        TopologicalAction ta1 = (TopologicalAction) it.next();
        TopologicalAction ta2 = (TopologicalAction) it.next();
        if ("BBE1AA1  BBE4AA1  1".equals(ta1.getNetworkElement().getName())) {
            assertEquals(ActionType.OPEN, ta1.getActionType());
            assertEquals(ActionType.CLOSE, ta2.getActionType());
        } else {
            assertEquals(ActionType.OPEN, ta2.getActionType());
            assertEquals(ActionType.CLOSE, ta1.getActionType());
        }
        assertRaNotImported(cracCreationContext, "no-static-property-range", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action no-static-property-range will not be imported because there is no StaticPropertyRange linked to elementary action b2976651-58f9-46bf-bd0d-a721b11dbb9a");
        assertRaNotImported(cracCreationContext, "wrong-value-offset-kind", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action wrong-value-offset-kind will not be imported because the ValueOffsetKind is http://entsoe.eu/ns/nc#ValueOffsetKind.incremental but should be none.");
        assertRaNotImported(cracCreationContext, "wrong-direction", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action wrong-direction will not be imported because the RelativeDirectionKind is http://entsoe.eu/ns/nc#RelativeDirectionKind.up but should be absolute.");
        assertRaNotImported(cracCreationContext, "undefined-action-type", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action undefined-action-type will not be imported because the normalValue is 2 which does not define a proper action type (open 1 / close 0)");
    }
}
