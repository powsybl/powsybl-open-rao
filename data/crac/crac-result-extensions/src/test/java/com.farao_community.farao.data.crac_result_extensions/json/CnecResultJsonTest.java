/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.*;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;

import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultJsonTest {

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
        simpleCrac.addCnec("cnec1prev", "ne1", new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30), preventiveState.getId());

        // One Cnec with extension
        simpleCrac.addNetworkElement("ne2");
        Cnec preventiveCnec1 = simpleCrac.addCnec("cnec2prev", "ne2", new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500), preventiveState.getId());
        preventiveCnec1.addExtension(CnecResult.class, new CnecResult(50.0, 75.0));

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
        assertNull(crac.getCnec("cnec1prev").getExtension(CnecResult.class));

        // assert that the second one has a CnecResult extension with the expected content
        assertEquals(1, crac.getCnec("cnec2prev").getExtensions().size());
        assertNotNull(crac.getCnec("cnec2prev").getExtension(CnecResult.class));
        assertEquals(50.0, crac.getCnec("cnec2prev").getExtension(CnecResult.class).getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, crac.getCnec("cnec2prev").getExtension(CnecResult.class).getFlowInA(), DOUBLE_TOLERANCE);
    }
}
