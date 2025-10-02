/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TopologicalActionCreationTest {

    @Test
    void importTopologicalActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/TopologicalActions.zip", NcCracCreationTestUtil.NETWORK);

        List<NetworkAction> importedTopologicalActions = cracCreationContext.getCrac().getNetworkActions().stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        assertEquals(2, importedTopologicalActions.size());

        NcCracCreationTestUtil.assertSimpleTopologicalActionImported(importedTopologicalActions.get(0), "remedial-action-1", "RTE_RA1", "BBE1AA1  BBE4AA1  1", ActionType.OPEN, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID);

        NcCracCreationTestUtil.assertSimpleTopologicalActionImported(importedTopologicalActions.get(1), "remedial-action-2", "RTE_RA2", "DDE3AA1  DDE4AA1  1", ActionType.CLOSE, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID);

        assertEquals(9, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-3", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-3 will not be imported because the network does not contain a switch with id: unknown-switch");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-4", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-4 will not be imported because it has no elementary action");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-5", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-5 will not be imported because TopologyAction must have a property reference with http://energy.referencedata.eu/PropertyReference/Switch.open value, but it was: http://energy.referencedata.eu/PropertyReference/RotatingMachine.p");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/Switch.open value, but it was: http://energy.referencedata.eu/PropertyReference/RotatingMachine.p");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because the RelativeDirectionKind is http://entsoe.eu/ns/nc#RelativeDirectionKind.up but should be none");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because the ValueOffsetKind is http://entsoe.eu/ns/nc#ValueOffsetKind.incrementalPercentage but should be absolute");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-9 will not be imported because there is no StaticPropertyRange linked to elementary action topology-action-9");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because the normalValue is 2.0 which does not define a proper action type (open 1 / close 0)");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because several conflictual StaticPropertyRanges are linked to elementary action topology-action-11");
    }
}
