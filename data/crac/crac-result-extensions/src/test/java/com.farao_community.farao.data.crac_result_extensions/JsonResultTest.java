/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
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

        // add a ResultVariantManager to the Crac
        simpleCrac.addExtension(ResultVariantManager.class, new ResultVariantManager());

        // add variants
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant1");
        simpleCrac.getExtension(ResultVariantManager.class).createVariant("variant2");

        // CracResult
        CracResultExtension cracResultExtension = simpleCrac.getExtension(CracResultExtension.class);
        cracResultExtension.getVariant("variant1").setCost(10);
        cracResultExtension.getVariant("variant1").setNetworkSecurityStatus();

        // CnecResult
        CnecResultExtension cnecResultExtension = simpleCrac.getCnec("cnec2prev").getExtension(CnecResultExtension.class);
        cnecResultExtension.getVariant("variant1").setFlowInA(75.0);
        cnecResultExtension.getVariant("variant1").setFlowInMW(50.0);
        cnecResultExtension.getVariant("variant2").setFlowInA(750.0);
        cnecResultExtension.getVariant("variant2").setFlowInMW(450.0);

        // PstRangeResult
        pstWithRange1.getExtension(RangeActionResultExtension.class).getVariant("variant1").setSetPoint(preventiveState.getId(), 4.0);
        ((PstRangeResult) pstWithRange1.getExtension(RangeActionResultExtension.class).getVariant("variant1")).setTap(preventiveState.getId(), 2);

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
        assertEquals(10.0, extCrac.getVariant("variant1").getCost(), DOUBLE_TOLERANCE);
        assertEquals(CracResult.NetworkSecurityStatus.UNSECURED, extCrac.getVariant("variant1").getNetworkSecurityStatus());

        // CnecResultExtension
        CnecResultExtension extCnec = crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(-450.0, extCnec.getVariant("variant2").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(750.0, extCnec.getVariant("variant2").getFlowInA(), DOUBLE_TOLERANCE);
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
