/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonResultTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void cracRoundTripTest() {

        Crac cracIn = CracFactory.findDefault().create("cracId", "cracName");

        // add cnecs
        cracIn.newFlowCnec()
            .withId("cnec1prev")
            .withNetworkElement("ne1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-500.)
                .add()
            .add();

        cracIn.newFlowCnec()
            .withId("cnec2prev")
            .withNetworkElement("ne2")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .add()
            .add();

        // add PstRangeAction
        cracIn.newPstRangeAction()
            .withId("pst1")
            .withNetworkElement("pst1NetworkElement")
            .add();

        // add topological action
        cracIn.newNetworkAction()
            .withId("topoRaId")
            .withName("topoRaName")
            .withOperator("operator")
            .newTopologicalAction()
                .withActionType(ActionType.CLOSE)
                .withNetworkElement("topologicalActionNetworkElement")
                .add()
            .add();

        // add Pst set point
        cracIn.newNetworkAction()
            .withId("pstSetPointRaId")
            .withName("pstSetPointRaName")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstSetPointNetworkElement")
                .withSetpoint(12)
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .add()
            .add();


        // add a ResultVariantManager to the Crac
        cracIn.addExtension(ResultVariantManager.class, new ResultVariantManager());

        // add variants
        cracIn.getExtension(ResultVariantManager.class).createVariant("variant1");
        cracIn.getExtension(ResultVariantManager.class).createVariant("variant2");

        // CracResult
        CracResultExtension cracResultExtension = cracIn.getExtension(CracResultExtension.class);
        cracResultExtension.getVariant("variant1").setFunctionalCost(10);
        cracResultExtension.getVariant("variant1").setNetworkSecurityStatus(CracResult.NetworkSecurityStatus.UNSECURED);

        // CnecResult
        CnecResultExtension cnecResultExtension = cracIn.getFlowCnec("cnec2prev").getExtension(CnecResultExtension.class);
        cnecResultExtension.getVariant("variant1").setFlowInA(75.0);
        cnecResultExtension.getVariant("variant1").setFlowInMW(50.0);
        cnecResultExtension.getVariant("variant1").setMinThresholdInMW(-1000);
        cnecResultExtension.getVariant("variant1").setMaxThresholdInMW(1000);
        cnecResultExtension.getVariant("variant1").setMinThresholdInA(-700);
        cnecResultExtension.getVariant("variant1").setMaxThresholdInA(700);
        cnecResultExtension.getVariant("variant1").setAbsolutePtdfSum(0.2);
        cnecResultExtension.getVariant("variant2").setFlowInA(750.0);
        cnecResultExtension.getVariant("variant2").setFlowInMW(450.0);

        String preventiveStateId = cracIn.getPreventiveState().getId();

        // PstRangeResult
        RangeActionResultExtension rangeActionResultExtension = cracIn.getRangeAction("pst1").getExtension(RangeActionResultExtension.class);
        double pstRangeSetPointVariant1 = 4.0;
        double pstRangeSetPointVariant2 = 14.0;
        Integer pstRangeTapVariant1 = 2;
        Integer pstRangeTapVariant2 = 6;
        rangeActionResultExtension.getVariant("variant1").setSetPoint(preventiveStateId, pstRangeSetPointVariant1);
        ((PstRangeResult) rangeActionResultExtension.getVariant("variant1")).setTap(preventiveStateId, pstRangeTapVariant1);
        rangeActionResultExtension.getVariant("variant2").setSetPoint(preventiveStateId, pstRangeSetPointVariant2);
        ((PstRangeResult) rangeActionResultExtension.getVariant("variant2")).setTap(preventiveStateId, pstRangeTapVariant2);

        // NetworkActionResult for topology
        NetworkActionResultExtension topologyResultExtension = cracIn.getNetworkAction("topoRaId").getExtension(NetworkActionResultExtension.class);
        topologyResultExtension.getVariant("variant1").activate(preventiveStateId);
        topologyResultExtension.getVariant("variant2").deactivate(preventiveStateId);

        // NetworkActionResult for pstSetpoint
        NetworkActionResultExtension pstSetpointResultExtension = cracIn.getNetworkAction("pstSetPointRaId").getExtension(NetworkActionResultExtension.class);
        pstSetpointResultExtension.getVariant("variant1").activate(preventiveStateId);
        pstSetpointResultExtension.getVariant("variant2").deactivate(preventiveStateId);

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(cracIn, "Json", outputStream);

        // import Crac
        Crac cracOut;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            cracOut = CracImporters.importCrac("unknown.json", inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // assert
        // assert that the ResultVariantManager exists and contains the expected results
        assertNotNull(cracOut.getExtension(ResultVariantManager.class));
        assertEquals(2, cracOut.getExtension(ResultVariantManager.class).getVariants().size());
        assertTrue(cracOut.getExtension(ResultVariantManager.class).getVariants().contains("variant1"));
        assertTrue(cracOut.getExtension(ResultVariantManager.class).getVariants().contains("variant2"));

        // assert that the CracResultExtension exists and contains the expected results
        assertNotNull(cracOut.getExtension(CracResultExtension.class));
        assertEquals(10.0, cracOut.getExtension(CracResultExtension.class).getVariant("variant1").getCost(), DOUBLE_TOLERANCE);
        assertEquals(10.0, cracOut.getExtension(CracResultExtension.class).getVariant("variant1").getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0.0, cracOut.getExtension(CracResultExtension.class).getVariant("variant1").getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, cracOut.getExtension(CracResultExtension.class).getVariant("variant1").getNetworkSecurityStatus());

        // assert that cnecs exist in the crac
        assertEquals(2, cracOut.getFlowCnecs().size());
        assertNotNull(cracOut.getFlowCnec("cnec1prev"));
        assertNotNull(cracOut.getFlowCnec("cnec2prev"));

        // assert that the second one has a CnecResult extension with the expected content
        assertEquals(1, cracOut.getFlowCnec("cnec2prev").getExtensions().size());
        CnecResultExtension extCnec = cracOut.getFlowCnec("cnec2prev").getExtension(CnecResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(50.0, extCnec.getVariant("variant1").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, extCnec.getVariant("variant1").getFlowInA(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, extCnec.getVariant("variant1").getMinThresholdInMW(),  DOUBLE_TOLERANCE);
        assertEquals(1000.0, extCnec.getVariant("variant1").getMaxThresholdInMW(),  DOUBLE_TOLERANCE);
        assertEquals(-700.0, extCnec.getVariant("variant1").getMinThresholdInA(),  DOUBLE_TOLERANCE);
        assertEquals(700.0, extCnec.getVariant("variant1").getMaxThresholdInA(),  DOUBLE_TOLERANCE);
        assertEquals(0.2, extCnec.getVariant("variant1").getAbsolutePtdfSum(), DOUBLE_TOLERANCE);

        // assert that the PstWithRange has a RangeActionResultExtension with the expected content
        assertEquals(1, cracOut.getRangeAction("pst1").getExtensions().size());
        RangeActionResultExtension rangeActionResultExtension1 = cracOut.getRangeAction("pst1").getExtension(RangeActionResultExtension.class);
        assertNotNull(rangeActionResultExtension1);
        assertEquals(pstRangeSetPointVariant1, rangeActionResultExtension1.getVariant("variant1").getSetPoint(preventiveStateId));
        assertEquals(pstRangeTapVariant1, ((PstRangeResult) rangeActionResultExtension1.getVariant("variant1")).getTap(preventiveStateId));
        assertEquals(pstRangeSetPointVariant2, rangeActionResultExtension1.getVariant("variant2").getSetPoint(preventiveStateId));
        assertEquals(pstRangeTapVariant2, ((PstRangeResult) rangeActionResultExtension1.getVariant("variant2")).getTap(preventiveStateId));

        // assert that the TopologicalActionImpl has a NetworkActionResultExtension with the expected content
        assertEquals(1, cracOut.getNetworkAction("topoRaId").getExtensions().size());
        NetworkActionResultExtension exportedTopologyResultExtension = cracOut.getNetworkAction("topoRaId").getExtension(NetworkActionResultExtension.class);
        assertNotNull(exportedTopologyResultExtension);
        assertTrue(exportedTopologyResultExtension.getVariant("variant1").isActivated(preventiveStateId));
        assertFalse(exportedTopologyResultExtension.getVariant("variant2").isActivated(preventiveStateId));

        // assert that the PstSetpointImpl has a NetworkActionResultExtension with the expected content
        assertEquals(1, cracOut.getNetworkAction("pstSetPointRaId").getExtensions().size());
        NetworkActionResultExtension exportedPstSetpointResultExtension = cracOut.getNetworkAction("pstSetPointRaId").getExtension(NetworkActionResultExtension.class);
        assertNotNull(exportedPstSetpointResultExtension);
        assertTrue(exportedPstSetpointResultExtension.getVariant("variant1").isActivated(preventiveStateId));
        assertFalse(exportedPstSetpointResultExtension.getVariant("variant2").isActivated(preventiveStateId));
    }

    @Test
    public void cracImportTest() {
        Crac crac = CracImporters.importCrac("small-crac-with-result-extensions.json", getClass().getResourceAsStream("/small-crac-with-result-extensions.json"));

        // ResultVariantManager
        assertNotNull(crac.getExtension(ResultVariantManager.class));
        assertEquals(2, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant1"));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant2"));

        // CracResultExtension
        CracResultExtension extCrac = crac.getExtension(CracResultExtension.class);
        assertNotNull(extCrac);
        assertEquals(15.0, extCrac.getVariant("variant1").getCost(), DOUBLE_TOLERANCE);
        assertEquals(10.0, extCrac.getVariant("variant1").getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(5.0, extCrac.getVariant("variant1").getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, extCrac.getVariant("variant1").getNetworkSecurityStatus());

        // CnecResultExtension
        CnecResultExtension extCnec = crac.getFlowCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(-450.0, extCnec.getVariant("variant2").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(750.0, extCnec.getVariant("variant2").getFlowInA(), DOUBLE_TOLERANCE);
        assertEquals(0.85, extCnec.getVariant("variant2").getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
    }

    @Test
    public void cracImportWithUnknownFieldInExtension() {
        try {
            CracImporters.importCrac("small-crac-errored.json", getClass().getResourceAsStream("/small-crac-errored.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }
}
