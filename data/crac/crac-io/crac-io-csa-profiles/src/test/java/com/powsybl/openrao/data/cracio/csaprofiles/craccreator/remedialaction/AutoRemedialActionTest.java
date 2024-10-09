/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.cracimpl.OnContingencyStateImpl;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRemedialActionTest {

    @Test
    void importAutoRemedialActions() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/profiles/remedialactions/AutoRemedialActions.zip", NETWORK);

        List<RemedialAction<?>> importedSps = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().size() == 1 && ra.getUsageRules().stream().toList().get(0).getInstant().isAuto()).toList();
        assertEquals(2, importedSps.size());

        PstRangeAction pstSps = cracCreationContext.getCrac().getPstRangeAction("pst-sps");
        OnContingencyStateImpl pstSpsUsageRule = (OnContingencyStateImpl) pstSps.getUsageRules().iterator().next();
        assertEquals("RTE_PST SPS", pstSps.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", pstSps.getNetworkElement().getId());
        assertEquals(-5, pstSps.getRanges().get(0).getMinTap());
        assertEquals(7, pstSps.getRanges().get(0).getMaxTap());
        assertEquals("contingency", pstSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, pstSpsUsageRule.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, pstSpsUsageRule.getUsageMethod());

        NetworkAction networkSps = cracCreationContext.getCrac().getNetworkAction("network-sps");
        OnContingencyStateImpl networkSpsUsageRule = (OnContingencyStateImpl) networkSps.getUsageRules().iterator().next();
        List<Action> elementaryActions = networkSps.getElementaryActions().stream().sorted(Comparator.comparing(Action::toString)).toList();
        GeneratorAction injectionSetpoint = (GeneratorAction) elementaryActions.get(0);
        SwitchAction topologicalAction = (SwitchAction) elementaryActions.get(1);
        assertEquals("RTE_Network SPS", networkSps.getName());
        assertEquals(2, elementaryActions.size());
        assertEquals("BBE1AA1  BBE4AA1  1", topologicalAction.getSwitchId());
        assertTrue(topologicalAction.isOpen());
        assertEquals("FFR1AA1 _generator", injectionSetpoint.getGeneratorId());
        assertEquals(75.0, injectionSetpoint.getActivePowerValue().getAsDouble());
        assertEquals("contingency", networkSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, networkSpsUsageRule.getInstant().getKind());
        assertEquals(UsageMethod.FORCED, networkSpsUsageRule.getUsageMethod());
    }
}
