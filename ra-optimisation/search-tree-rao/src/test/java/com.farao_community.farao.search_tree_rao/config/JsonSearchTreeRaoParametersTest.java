/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

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
public class JsonSearchTreeRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        parameters.getExtension(SearchTreeRaoParameters.class).setRangeActionRao("myRangeActionRao");
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/SearchTreeRaoParameters.json");
    }

    @Test
    public void readError() throws IOException {
        try {
            JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParametersError.json"));
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }
}
