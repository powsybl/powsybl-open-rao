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

        // One Cnec without extension
        simpleCrac.addNetworkElement("ne1");
        simpleCrac.addCnec("cnec1prev", "ne1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());

        // One Cnec with extension
        simpleCrac.addNetworkElement("ne2");
        Cnec preventiveCnec2 = simpleCrac.addCnec("cnec2prev", "ne2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());

        ResultExtension<Cnec, CnecResult> resultExtension = new ResultExtension<>();
        resultExtension.addVariant("variant1", new CnecResult(50.0, 75.0));
        resultExtension.addVariant("variant2", new CnecResult(450.0, 750.0));
        preventiveCnec2.addExtension(ResultExtension.class, resultExtension);

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
        // assert that cnecs exist in the crac
        assertEquals(2, crac.getCnecs().size());
        assertNotNull(crac.getCnec("cnec1prev"));
        assertNotNull(crac.getCnec("cnec2prev"));

        // assert that the first one has no extension
        assertTrue(crac.getCnec("cnec1prev").getExtensions().isEmpty());

        // assert that the second one has a CnecResult extension with the expected content
        assertEquals(1, crac.getCnec("cnec2prev").getExtensions().size());
        ResultExtension<Cnec, CnecResult> extCnec = crac.getCnec("cnec2prev").getExtension(ResultExtension.class);
        assertNotNull(extCnec);
        assertEquals(50.0, extCnec.getVariant("variant1").getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, extCnec.getVariant("variant1").getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void cracImportTest() {
        Crac crac = CracImporters.importCrac("small-crac-with-cnec-result.json", getClass().getResourceAsStream("/small-crac-with-cnec-result.json"));

        ResultExtension<Cnec, CnecResult> extCnec = crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(ResultExtension.class);

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
