/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_impl.InjectionSetpointImpl;
import com.farao_community.farao.data.crac_impl.OnFlowConstraintImpl;
import com.farao_community.farao.data.crac_impl.PstSetpointImpl;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * Test importing old versions of the json crac file
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class JsonRetrocompatibilityTest {

    private Crac importCrac(String filename) {
        return (new JsonImport()).importCrac(getClass().getResourceAsStream("/retrocompatibility/" + filename));
    }

    @Test
    public void testImportV1_0() {
        Crac importedCrac = importCrac("1_0.json");

        assertEquals(3, importedCrac.getStates().size());
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(6, importedCrac.getFlowCnecs().size());
        assertEquals(4, importedCrac.getRangeActions().size());
        assertEquals(3, importedCrac.getNetworkActions().size());
        assertEquals(4, importedCrac.getFlowCnec("cnec2prev").getThresholds().size());
        assertFalse(importedCrac.getFlowCnec("cnec3prevId").isOptimized());
        assertTrue(importedCrac.getFlowCnec("cnec4prevId").isMonitored());
        assertEquals(1, importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpointImpl);
        assertEquals(1, importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpointImpl);
        assertEquals(2, importedCrac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());
        assertEquals("group-1", importedCrac.getRangeAction("pstRangeId2").getGroupId().orElseThrow());
        assertEquals("group-1", importedCrac.getRangeAction("hvdcRangeId2").getGroupId().orElseThrow());
        assertTrue(importedCrac.getRangeAction("pstRangeId").getGroupId().isEmpty());
        assertTrue(importedCrac.getRangeAction("hvdcRangeId").getGroupId().isEmpty());

        assertEquals("operator1", importedCrac.getFlowCnec("cnec1prev").getOperator());
        assertEquals("operator1", importedCrac.getFlowCnec("cnec1cur").getOperator());
        assertEquals("operator2", importedCrac.getFlowCnec("cnec2prev").getOperator());
        assertEquals("operator3", importedCrac.getFlowCnec("cnec3prevId").getOperator());
        assertEquals("operator4", importedCrac.getFlowCnec("cnec4prevId").getOperator());

        assertEquals(2, importedCrac.getPstRangeAction("pstRangeId").getInitialTap());
        assertEquals(0.5, importedCrac.getPstRangeAction("pstRangeId").convertTapToAngle(-2));
        assertEquals(2.5, importedCrac.getPstRangeAction("pstRangeId").convertTapToAngle(2));
        assertEquals(2, importedCrac.getPstRangeAction("pstRangeId").convertAngleToTap(2.5));

        assertEquals(1, importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().size());
        assertTrue(importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().get(0) instanceof OnFlowConstraintImpl);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertSame(importedCrac.getCnec("cnec3prevId"), onFlowConstraint.getFlowCnec());

        assertEquals(1, importedCrac.getHvdcRangeAction("hvdcRangeId2").getUsageRules().size());
        assertTrue(importedCrac.getHvdcRangeAction("hvdcRangeId2").getUsageRules().get(0) instanceof OnFlowConstraintImpl);
        OnFlowConstraint onFlowConstraint2 = (OnFlowConstraint) importedCrac.getHvdcRangeAction("hvdcRangeId2").getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint2.getInstant());
        assertSame(importedCrac.getCnec("cnec3prevIdBis"), onFlowConstraint2.getFlowCnec());
    }
}
