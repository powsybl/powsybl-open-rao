/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.CURATIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertHasOnInstantUsageRule;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertPstRangeActionImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.assertRaNotImported;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PstRangeActionCreationTest {

    @Test
    void importPstRangeActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/PSTRangeActions.zip", NETWORK);

        List<PstRangeAction> importedPstRangeActions = cracCreationContext.getCrac().getPstRangeActions().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        assertEquals(4, importedPstRangeActions.size());

        assertPstRangeActionImported(importedPstRangeActions.get(0), "remedial-action-1", "RTE_RA1", "BBE2AA1  BBE3AA1  1", null, null, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-1", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(1), "remedial-action-2", "RTE_RA2", "FFR2AA1  FFR4AA1  1", 3, null, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-2", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(2), "remedial-action-3", "RTE_RA3", "FFR2AA1  FFR4AA1  1", null, 17, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-3", CURATIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertPstRangeActionImported(importedPstRangeActions.get(3), "remedial-action-4", "RTE_RA4", "BBE2AA1  BBE3AA1  1", 5, 15, "RTE");
        assertHasOnInstantUsageRule(cracCreationContext, "remedial-action-4", PREVENTIVE_INSTANT_ID, UsageMethod.AVAILABLE);

        assertEquals(8, cracCreationContext.getRemedialActionCreationContexts().stream().filter(context -> !context.isImported()).toList().size());

        assertRaNotImported(cracCreationContext, "remedial-action-5", ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action remedial-action-5 will not be imported because transformer with id unknown-pst was not found in network");
        assertRaNotImported(cracCreationContext, "remedial-action-6", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-6 will not be imported because TapPositionAction must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-7", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-7 will not be imported because StaticPropertyRange must have a property reference with http://energy.referencedata.eu/PropertyReference/TapChanger.step value, but it was: http://energy.referencedata.eu/PropertyReference/Switch.open");
        assertRaNotImported(cracCreationContext, "remedial-action-8", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-8 will not be imported because several TapPositionActions were defined for the same PST Range Action when only one is expected");
        assertRaNotImported(cracCreationContext, "remedial-action-9", ImportStatus.NOT_FOR_RAO, "Remedial action remedial-action-9 will not be imported because the field normalEnabled in TapPositionAction is set to false");
        assertRaNotImported(cracCreationContext, "remedial-action-10", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-10 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
        assertRaNotImported(cracCreationContext, "remedial-action-11", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-11 will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down");
        assertRaNotImported(cracCreationContext, "remedial-action-12", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action remedial-action-12 will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute");
    }
}
