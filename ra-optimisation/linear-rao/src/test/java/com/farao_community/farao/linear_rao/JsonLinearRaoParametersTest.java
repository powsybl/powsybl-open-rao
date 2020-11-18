/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonLinearRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(LinearRaoParameters.class, new LinearRaoParameters());
        parameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(true);
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/LinearRaoParameters.json");
    }

    @Test
    public void readUnknownField() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/LinearRaoParametersUnknownField.json")) {
            JsonRaoParameters.read(is);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    @Test
    public void readExtension() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersWithBoundaries.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(RaoPtdfParameters.class));
        assertNotNull(parameters.getExtensionByName("RaoPtdfParameters"));
        RaoPtdfParameters extension = parameters.getExtension(RaoPtdfParameters.class);
        assertEquals(0.07, extension.getPtdfSumLowerBound(), 1e-6);
        assertEquals(2, extension.getBoundaries().size());
        assertTrue(extension.getBoundariesAsString().contains("FR-ES"));
        assertTrue(extension.getBoundariesAsString().contains("ES-PT"));
    }

    @Test
    public void update() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_default.json"));
        JsonRaoParameters.update(parameters, getClass().getResourceAsStream("/RaoParameters_relMargin_ampere.json"));
        RaoPtdfParameters extension = parameters.getExtension(RaoPtdfParameters.class);
        assertNotNull(extension);
        assertEquals(0.01, extension.getPtdfSumLowerBound(), 1e-6);
        assertEquals(2, extension.getBoundaries().size());
        assertTrue(extension.getBoundariesAsString().contains("FR-ES"));
        assertTrue(extension.getBoundariesAsString().contains("ES-PT"));
    }
}
