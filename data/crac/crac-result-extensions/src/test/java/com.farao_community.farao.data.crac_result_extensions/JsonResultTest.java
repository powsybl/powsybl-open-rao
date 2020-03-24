/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.*;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

        // add a ResultVariantManager to the Crac
        Set<String> variantIds = new HashSet<>();
        variantIds.add("variant1");
        variantIds.add("variant2");
        simpleCrac.addExtension(ResultVariantManager.class, new ResultVariantManager(variantIds));

        // add a CracResultExtension to the Crac
        CracResultExtension cracCracResultResultExtension = new CracResultExtension();
        cracCracResultResultExtension.addVariant("variant1", new CracResult(CracResult.NetworkSecurityStatus.UNSECURED, 10));
        simpleCrac.addExtension(CracResultExtension.class, cracCracResultResultExtension);

        // States
        Instant initialInstant = simpleCrac.addInstant("N", 0);
        State preventiveState = simpleCrac.addState(null, initialInstant);

        // One Cnec without extension
        simpleCrac.addNetworkElement("ne1");
        simpleCrac.addCnec("cnec1prev", "ne1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());

        // One Cnec with extension
        simpleCrac.addNetworkElement("ne2");
        Cnec preventiveCnec2 = simpleCrac.addCnec("cnec2prev", "ne2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());

        CnecResultExtension resultExtension = new CnecResultExtension();
        resultExtension.addVariant("variant1", new CnecResult(50.0, 75.0));
        resultExtension.addVariant("variant2", new CnecResult(450.0, 750.0));
        preventiveCnec2.addExtension(CnecResultExtension.class, resultExtension);

        // Add a PstWithRange without extension
        NetworkElement networkElement1 = new NetworkElement("pst1networkElement");
        simpleCrac.addNetworkElement("pst1networkElement");
        PstWithRange pstWithRange1 = new PstWithRange("pst1", networkElement1);
        simpleCrac.addRangeAction(pstWithRange1);

        // Add a PstWithRange with extension
        NetworkElement networkElement2 = new NetworkElement("pst2networkElement");
        simpleCrac.addNetworkElement("pst2networkElement");
        PstWithRange pstWithRange2 = new PstWithRange("pst2", networkElement2);
        simpleCrac.addRangeAction(pstWithRange2);
        RangeActionResultExtension rangeActionResultExtension = new RangeActionResultExtension();
        HashMap<String, Integer> tapsPerState = new HashMap<>();
        HashMap<String, Double> setPointPerStates = new HashMap<>();
        PstRangeResult pstRangeResult = new PstRangeResult(setPointPerStates, tapsPerState);
        pstRangeResult.setTap(preventiveState.getId(), 2);
        pstRangeResult.setSetPoint(preventiveState.getId(), 4.0);
        rangeActionResultExtension.addVariant("variant1", pstRangeResult);
        pstWithRange2.addExtension(RangeActionResultExtension.class, rangeActionResultExtension);

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

        // assert that the first one has no extension
        assertTrue(crac.getCnec("cnec1prev").getExtensions().isEmpty());

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
