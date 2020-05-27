/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonLinearRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(LinearRaoParameters.class, new LinearRaoParameters());
        parameters.getExtension(LinearRaoParameters.class).setObjectiveFunction(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        parameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(true);
        parameters.getExtension(LinearRaoParameters.class).setFallbackOvercost(100.0);
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/LinearRaoParameters.json");
    }

    @Test
    public void readUnknownField() {
        try {
            JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersUnknownField.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    @Test
    public void readError() {
        try {
            JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersUnknownObjective.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unknown objective"));
        }
    }
}
