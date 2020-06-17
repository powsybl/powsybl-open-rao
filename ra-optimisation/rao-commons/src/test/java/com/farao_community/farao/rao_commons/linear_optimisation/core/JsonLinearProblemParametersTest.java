/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class JsonLinearProblemParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(LinearProblemParameters.class, new LinearProblemParameters());
        parameters.getExtension(LinearProblemParameters.class).setPstSensitivityThreshold(1.0);
        parameters.getExtension(LinearProblemParameters.class).setPstPenaltyCost(0.5);
        parameters.getExtension(LinearProblemParameters.class).setObjectiveFunction(LinearProblemParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        parameters.getExtension(LinearProblemParameters.class).setLoopflowConstraintAdjustmentCoefficient(1.0);
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/LinearProblemParameters.json");
    }

    @Test
    public void readUnknownField() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/LinearProblemParametersUnknownField.json")) {
            JsonRaoParameters.read(is);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    @Test
    public void readError() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/LinearProblemParametersUnknownObjective.json")) {
            JsonRaoParameters.read(is);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unknown objective"));
        }
    }
}
