/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.impl.OnContingencyStateImpl;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRemedialActionTest {

    @Test
    void importAutoRemedialActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext("/profiles/remedialactions/AutoRemedialActions.zip", NcCracCreationTestUtil.NETWORK);

        List<RemedialAction<?>> importedSps = cracCreationContext.getCrac().getRemedialActions().stream().filter(ra -> ra.getUsageRules().size() == 1 && ra.getUsageRules().stream().toList().get(0).getInstant().isAuto()).toList();
        assertEquals(2, importedSps.size());

        PstRangeAction pstSps = cracCreationContext.getCrac().getPstRangeAction("pst-sps");
        OnContingencyStateImpl pstSpsUsageRule = (OnContingencyStateImpl) pstSps.getUsageRules().iterator().next();
        assertEquals("PST SPS", pstSps.getName());
        assertEquals("BBE2AA1  BBE3AA1  1", pstSps.getNetworkElement().getId());
        assertEquals(-5, pstSps.getRanges().get(0).getMinTap());
        assertEquals(7, pstSps.getRanges().get(0).getMaxTap());
        assertEquals("contingency", pstSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, pstSpsUsageRule.getInstant().getKind());

        NetworkAction networkSps = cracCreationContext.getCrac().getNetworkAction("network-sps");
        OnContingencyStateImpl networkSpsUsageRule = (OnContingencyStateImpl) networkSps.getUsageRules().iterator().next();
        List<Action> elementaryActions = networkSps.getElementaryActions().stream().sorted(Comparator.comparing(Action::toString)).toList();
        GeneratorAction injectionSetpoint = (GeneratorAction) elementaryActions.get(0);
        SwitchAction topologicalAction = (SwitchAction) elementaryActions.get(1);
        assertEquals("Network SPS", networkSps.getName());
        assertEquals(2, elementaryActions.size());
        assertEquals("BBE1AA1  BBE4AA1  1", topologicalAction.getSwitchId());
        assertTrue(topologicalAction.isOpen());
        assertEquals("FFR1AA1 _generator", injectionSetpoint.getGeneratorId());
        assertEquals(75.0, injectionSetpoint.getActivePowerValue().getAsDouble());
        assertEquals("contingency", networkSpsUsageRule.getContingency().getId());
        assertEquals(InstantKind.AUTO, networkSpsUsageRule.getInstant().getKind());

        assertEquals(3, cracCreationContext.getRemedialActionCreationContexts().stream().filter(ra -> !ra.isImported()).toList().size());

        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "pst-sps-without-speed", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action pst-sps-without-speed will not be imported because an auto PST range action must have a speed defined");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "preventive-sps", ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "OpenRAO does not support preventive automatons, remedial action preventive-sps will be ignored");
        NcCracCreationTestUtil.assertRaNotImported(cracCreationContext, "sps-without-contingency", ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action sps-without-contingency will not be imported because no contingency or assessed element is linked to the remedial action and this is nor supported for ARAs");
    }
}
