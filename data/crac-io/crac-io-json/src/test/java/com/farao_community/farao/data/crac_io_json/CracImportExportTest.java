/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        Crac crac = ExhaustiveCracCreation.create();

        Crac importedCrac = RoundTripUtil.roundTrip(crac);

        // check overall content
        assertNotNull(importedCrac);
        assertEquals(5, importedCrac.getStates().size());
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(7, importedCrac.getFlowCnecs().size());
        assertEquals(4, importedCrac.getRangeActions().size());
        assertEquals(4, importedCrac.getNetworkActions().size());

        // check FlowCnec
        assertEquals(4, importedCrac.getFlowCnec("cnec2prevId").getThresholds().size());
        assertFalse(importedCrac.getFlowCnec("cnec3prevId").isOptimized());
        assertTrue(importedCrac.getFlowCnec("cnec4prevId").isMonitored());

        assertEquals("operator1", importedCrac.getFlowCnec("cnec1prevId").getOperator());
        assertEquals("operator1", importedCrac.getFlowCnec("cnec1outageId").getOperator());
        assertEquals("operator2", importedCrac.getFlowCnec("cnec2prevId").getOperator());
        assertEquals("operator3", importedCrac.getFlowCnec("cnec3prevId").getOperator());
        assertEquals("operator4", importedCrac.getFlowCnec("cnec4prevId").getOperator());

        // check NetworkActions
        assertEquals(1, importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpoint);
        assertEquals(1, importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpoint);
        assertEquals(2, importedCrac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());

        assertEquals(1, importedCrac.getNetworkAction("switchPairRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next() instanceof SwitchPair);
        SwitchPair switchPair = (SwitchPair) importedCrac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next();
        assertEquals("to-open", switchPair.getSwitchToOpen().getId());
        assertEquals("to-open", switchPair.getSwitchToOpen().getName());
        assertEquals("to-close", switchPair.getSwitchToClose().getId());
        assertEquals("to-close-name", switchPair.getSwitchToClose().getName());

        // check PstRangeActions
        assertTrue(importedCrac.getRangeAction("pstRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-pst", importedCrac.getRangeAction("pstRange2Id").getGroupId().orElseThrow());

        assertEquals(2, importedCrac.getPstRangeAction("pstRange1Id").getInitialTap());
        assertEquals(0.5, importedCrac.getPstRangeAction("pstRange1Id").convertTapToAngle(-2));
        assertEquals(2.5, importedCrac.getPstRangeAction("pstRange1Id").convertTapToAngle(2));
        assertEquals(2, importedCrac.getPstRangeAction("pstRange1Id").convertAngleToTap(2.5));

        assertEquals(1, importedCrac.getPstRangeAction("pstRange2Id").getUsageRules().size());
        assertTrue(importedCrac.getPstRangeAction("pstRange2Id").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) importedCrac.getPstRangeAction("pstRange2Id").getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertSame(importedCrac.getCnec("cnec3prevId"), onFlowConstraint.getFlowCnec());

        // check HvdcRangeActions
        assertTrue(importedCrac.getRangeAction("hvdcRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-hvdc", importedCrac.getRangeAction("hvdcRange2Id").getGroupId().orElseThrow());

        assertEquals(1, importedCrac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().size());
        assertTrue(importedCrac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint2 = (OnFlowConstraint) importedCrac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint2.getInstant());
        assertSame(importedCrac.getCnec("cnec3curId"), onFlowConstraint2.getFlowCnec());
    }
}
