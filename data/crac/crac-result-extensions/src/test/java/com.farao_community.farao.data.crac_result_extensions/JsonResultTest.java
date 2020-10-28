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
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonResultTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void cracRoundTripTest() {
        // Crac
        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        // States
        Instant initialInstant = simpleCrac.addInstant("N", 0);
        State preventiveState = simpleCrac.addState(null, initialInstant);

        // Cnecs
        simpleCrac.addNetworkElement("ne1");
        simpleCrac.addNetworkElement("ne2");
        simpleCrac.addCnec("cnec1prev", "ne1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());
        simpleCrac.addCnec("cnec2prev", "ne2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());

        // RangeActions : PstWithRange
        NetworkElement networkElement1 = new NetworkElement("pst1networkElement");
        simpleCrac.addNetworkElement("pst1networkElement");
        PstWithRange pstWithRange1 = new PstWithRange("pst1", networkElement1);
        simpleCrac.addRangeAction(pstWithRange1);

        // NetworkActions:
        // Topology
        NetworkElement networkElement2 = new NetworkElement("networkActionNetworkElement");
        simpleCrac.addNetworkElement(networkElement2);
        Topology topology = new Topology("topology", networkElement2, ActionType.CLOSE);
        simpleCrac.addNetworkAction(topology);

        // PstSetpoint
        PstSetpoint pstSetpoint = new PstSetpoint("pstSetpoint", networkElement2, 12.0, RangeDefinition.CENTERED_ON_ZERO);
        simpleCrac.addNetworkAction(pstSetpoint);

        // add a ResultVariantManager to the Crac
        simpleCrac.addExtension(ResultVariantManager.class, new ResultVariantManager());

        // add variants
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant1");
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant2");

        // CracResult
        CracResultExtension cracResultExtension = simpleCrac.getExtension(CracResultExtension.class);
        cracResultExtension.getVariant("variant1").setFunctionalCost(10);
        cracResultExtension.getVariant("variant1").setNetworkSecurityStatus(CracResult.NetworkSecurityStatus.UNSECURED);

        // CnecResult
        CnecResultExtension cnecResultExtension = simpleCrac.getCnec("cnec2prev").getExtension(CnecResultExtension.class);
        cnecResultExtension.getVariant("variant1").setFlowInA(75.0);
        cnecResultExtension.getVariant("variant1").setFlowInMW(50.0);
        cnecResultExtension.getVariant("variant1").setMinThresholdInMW(-1000);
        cnecResultExtension.getVariant("variant1").setMaxThresholdInMW(1000);
        cnecResultExtension.getVariant("variant1").setMinThresholdInA(-700);
        cnecResultExtension.getVariant("variant1").setMaxThresholdInA(700);
        cnecResultExtension.getVariant("variant1").setAbsolutePtdfSum(0.2);
        cnecResultExtension.getVariant("variant2").setFlowInA(750.0);
        cnecResultExtension.getVariant("variant2").setFlowInMW(450.0);

        // PstRangeResult
        RangeActionResultExtension rangeActionResultExtension = simpleCrac.getRangeAction("pst1").getExtension(RangeActionResultExtension.class);
        double pstRangeSetPointVariant1 = 4.0;
        double pstRangeSetPointVariant2 = 14.0;
        int pstRangeTapVariant1 = 2;
        int pstRangeTapVariant2 = 6;
        rangeActionResultExtension.getVariant("variant1").setSetPoint(preventiveState.getId(), pstRangeSetPointVariant1);
        ((PstRangeResult) rangeActionResultExtension.getVariant("variant1")).setTap(preventiveState.getId(), pstRangeTapVariant1);
        rangeActionResultExtension.getVariant("variant2").setSetPoint(preventiveState.getId(), pstRangeSetPointVariant2);
        ((PstRangeResult) rangeActionResultExtension.getVariant("variant2")).setTap(preventiveState.getId(), pstRangeTapVariant2);

        // NetworkActionResult for topology
        NetworkActionResultExtension topologyResultExtension = simpleCrac.getNetworkAction("topology").getExtension(NetworkActionResultExtension.class);
        topologyResultExtension.getVariant("variant1").activate(preventiveState.getId());
        topologyResultExtension.getVariant("variant2").deactivate(preventiveState.getId());

        // NetworkActionResult for pstSetpoint
        NetworkActionResultExtension pstSetpointResultExtension = simpleCrac.getNetworkAction("pstSetpoint").getExtension(NetworkActionResultExtension.class);
        pstSetpointResultExtension.getVariant("variant1").activate(preventiveState.getId());
        pstSetpointResultExtension.getVariant("variant2").deactivate(preventiveState.getId());

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(simpleCrac, "Json", outputStream);

        // import Crac
        Crac crac;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            crac = CracImporters.importCrac("unknown.json", inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // assert
        // assert that the ResultVariantManager exists and contains the expected results
        assertNotNull(crac.getExtension(ResultVariantManager.class));
        assertEquals(2, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant1"));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains("variant2"));

        // assert that the CracResultExtension exists and contains the expected results
        assertNotNull(crac.getExtension(CracResultExtension.class));
        assertEquals(10.0, crac.getExtension(CracResultExtension.class).getVariant("variant1").getCost(), DOUBLE_TOLERANCE);
        assertEquals(10.0, crac.getExtension(CracResultExtension.class).getVariant("variant1").getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0.0, crac.getExtension(CracResultExtension.class).getVariant("variant1").getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, crac.getExtension(CracResultExtension.class).getVariant("variant1").getNetworkSecurityStatus());

        // assert that cnecs exist in the crac
        assertEquals(2, crac.getCnecs().size());
        assertNotNull(crac.getCnec("cnec1prev"));
        assertNotNull(crac.getCnec("cnec2prev"));

        // assert that the second one has a CnecResult extension with the expected content
        assertEquals(1, crac.getCnec("cnec2prev").getExtensions().size());
        CnecResultExtension extCnec = crac.getCnec("cnec2prev").getExtension(CnecResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(50.0, extCnec.getVariant("variant1").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, extCnec.getVariant("variant1").getFlowInA(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, extCnec.getVariant("variant1").getMinThresholdInMW(),  DOUBLE_TOLERANCE);
        assertEquals(1000.0, extCnec.getVariant("variant1").getMaxThresholdInMW(),  DOUBLE_TOLERANCE);
        assertEquals(-700.0, extCnec.getVariant("variant1").getMinThresholdInA(),  DOUBLE_TOLERANCE);
        assertEquals(700.0, extCnec.getVariant("variant1").getMaxThresholdInA(),  DOUBLE_TOLERANCE);
        assertEquals(0.2, extCnec.getVariant("variant1").getAbsolutePtdfSum(), DOUBLE_TOLERANCE);

        // assert that the PstWithRange has a RangeActionResultExtension with the expected content
        assertEquals(1, crac.getRangeAction("pst1").getExtensions().size());
        RangeActionResultExtension rangeActionResultExtension1 = crac.getRangeAction("pst1").getExtension(RangeActionResultExtension.class);
        assertNotNull(rangeActionResultExtension1);
        assertEquals(pstRangeSetPointVariant1, rangeActionResultExtension1.getVariant("variant1").getSetPoint(preventiveState.getId()));
        assertEquals(pstRangeTapVariant1, ((PstRangeResult) rangeActionResultExtension1.getVariant("variant1")).getTap(preventiveState.getId()));
        assertEquals(pstRangeSetPointVariant2, rangeActionResultExtension1.getVariant("variant2").getSetPoint(preventiveState.getId()));
        assertEquals(pstRangeTapVariant2, ((PstRangeResult) rangeActionResultExtension1.getVariant("variant2")).getTap(preventiveState.getId()));

        // assert that the Topology has a NetworkActionResultExtension with the expected content
        assertEquals(1, crac.getNetworkAction("topology").getExtensions().size());
        NetworkActionResultExtension exportedTopologyResultExtension = crac.getNetworkAction("topology").getExtension(NetworkActionResultExtension.class);
        assertNotNull(exportedTopologyResultExtension);
        assertTrue(exportedTopologyResultExtension.getVariant("variant1").isActivated(preventiveState.getId()));
        assertFalse(exportedTopologyResultExtension.getVariant("variant2").isActivated(preventiveState.getId()));

        // assert that the PstSetpoint has a NetworkActionResultExtension with the expected content
        assertEquals(1, crac.getNetworkAction("pstSetpoint").getExtensions().size());
        NetworkActionResultExtension exportedPstSetpointResultExtension = crac.getNetworkAction("pstSetpoint").getExtension(NetworkActionResultExtension.class);
        assertNotNull(exportedPstSetpointResultExtension);
        assertTrue(exportedPstSetpointResultExtension.getVariant("variant1").isActivated(preventiveState.getId()));
        assertFalse(exportedPstSetpointResultExtension.getVariant("variant2").isActivated(preventiveState.getId()));
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
        CnecResultExtension extCnec = crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(-450.0, extCnec.getVariant("variant2").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(750.0, extCnec.getVariant("variant2").getFlowInA(), DOUBLE_TOLERANCE);
        assertEquals(0.85, extCnec.getVariant("variant2").getAbsolutePtdfSum(), DOUBLE_TOLERANCE);
    }

    @Test
    public void cracImportWithUnknownFieldInExtension() {
        try {
            Crac crac = CracImporters.importCrac("small-crac-errored.json", getClass().getResourceAsStream("/small-crac-errored.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }
}
