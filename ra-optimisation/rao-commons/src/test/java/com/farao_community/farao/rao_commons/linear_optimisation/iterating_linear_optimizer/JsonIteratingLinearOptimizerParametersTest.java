/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class JsonIteratingLinearOptimizerParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(IteratingLinearOptimizerParameters.class, new IteratingLinearOptimizerParameters());
        parameters.getExtension(IteratingLinearOptimizerParameters.class).setMaxIterations(20);
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/IteratingLinearOptimizerParameters.json");
    }

    @Test
    public void readUnknownField() {
        try {
            JsonRaoParameters.read(getClass().getResourceAsStream("/IteratingLinearOptimizerParametersUnknownField.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }
}
