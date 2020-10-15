/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class JsonRaoPtdfParametersTest extends AbstractConverterTest {
    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(RaoPtdfParameters.class, new RaoPtdfParameters());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR-ES", "ES-PT"));
        parameters.getExtension(RaoPtdfParameters.class).setBoundariesFromCountryCodes(stringBoundaries);
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoPtdfParameters.json");
    }

    @Test(expected = FaraoException.class)
    public void readUnknownField() throws IOException {
        InputStream is = getClass().getResourceAsStream("/RaoPtdfParametersUnknownField.json");
        JsonRaoParameters.read(is);
    }
}
