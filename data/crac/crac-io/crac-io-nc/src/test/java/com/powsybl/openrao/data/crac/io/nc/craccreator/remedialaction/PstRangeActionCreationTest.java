/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PstRangeActionCreationTest {

    @Test
    void importPstRangeActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/PSTRangeActions.zip", NcCracCreationTestUtil.NETWORK);

        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(6, importedPstRangeActions.size());

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(0), "remedial-action-1", "RTE_RA1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(1), "remedial-action-2", "RTE_RA2", "FFR2AA1  FFR4AA1  1", 3, null, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(2), "remedial-action-3", "RTE_RA3", "FFR2AA1  FFR4AA1  1", null, 17, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", NcCracCreationTestUtil.CURATIVE_1_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", NcCracCreationTestUtil.CURATIVE_2_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", NcCracCreationTestUtil.CURATIVE_3_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(3), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", 5, 15, "RTE");
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(4), "tap-position-action-8-1", "tap-position-action-8-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(5), "tap-position-action-8-2", "tap-position-action-8-2", "FFR2AA1  FFR4AA1  1", null, null, null);

        assertEquals(7, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-5", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-5 will not be imported because no PowerTransformer was found in the network for TapChanger unknown-tap-changer");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because TapPositionAction must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-9 will not be imported because the field normalEnabled in TapPositionAction is set to false");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "remedial-action-12", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-12 will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute");
    }

    @Test
    void checkAlignedPstsImport() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/AlignedPSTs.zip", NcCracCreationTestUtil.NETWORK);
        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(4, importedPstRangeActions.size());

        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(0), "pst-range-action-4", "PST 4", "FFR2AA1  FFR4AA1  1", null, null, null);
        assertEquals(Optional.empty(), importedPstRangeActions.get(0).getGroupId());
        assertEquals(Optional.empty(), importedPstRangeActions.get(0).getSpeed());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "pst-range-action-4", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(1), "tap-position-action-1-1", "RTE-tap-position-action-1-1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertEquals("pst-group-1", importedPstRangeActions.get(1).getGroupId().get());
        assertEquals(2, importedPstRangeActions.get(1).getSpeed().get());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-1-1", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(2), "tap-position-action-1-2", "RTE-tap-position-action-1-2", "FFR2AA1  FFR4AA1  1", null, null, "RTE");
        assertEquals("pst-group-1", importedPstRangeActions.get(2).getGroupId().get());
        assertEquals(2, importedPstRangeActions.get(2).getSpeed().get());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-1-2", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        NcCracCreationTestUtil.assertPstRangeActionImported(importedPstRangeActions.get(3), "tap-position-action-2-1", "tap-position-action-2-1", "BBE2AA1  BBE3AA1  1", null, null, null);
        assertEquals("pst-group-2", importedPstRangeActions.get(3).getGroupId().get());
        assertEquals(3, importedPstRangeActions.get(3).getSpeed().get());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "tap-position-action-2-1", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);
        assertEquals(2, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "tap-position-action-2-2", ImportStatus.NOT_FOR_RAO, "Remedial action tap-position-action-2-2 will not be imported because the field normalEnabled in TapPositionAction is set to false");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "pst-group-3", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action tap-position-action-3-2 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
    }

}
