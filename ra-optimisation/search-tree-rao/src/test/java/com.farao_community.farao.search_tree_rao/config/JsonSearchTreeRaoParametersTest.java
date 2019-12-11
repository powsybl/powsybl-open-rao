/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonSearchTreeRaoParametersTest {
    private RaoParameters parameters;

    @Before
    public void setUp() {
        parameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        searchTreeRaoParameters.setRangeActionRao("myRangeActionRao");
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
    }

    @Test
    public void shouldImportAValidParametersFileAndHaveCorrectsValues() {
        RaoParameters importedParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/config/raoParametersValid.json"));
        SearchTreeRaoParameters searchTreeRaoParameters = importedParameters.getExtension(SearchTreeRaoParameters.class);
        assertEquals(searchTreeRaoParameters.getRangeActionRao(), "myRangeActionRao");
    }

    @Test
    public void shouldExportParametersInAValidFormat() throws IOException {
        String expectedParameters;
        try (InputStream is = getClass().getResourceAsStream("/config/raoParametersValid.json")) {
            expectedParameters = IOUtils.toString(is, "UTF-8");
        }
        String actualParameters;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonRaoParameters.write(parameters, baos);
            actualParameters = new String(baos.toByteArray());
        }
        assertEquals(actualParameters, expectedParameters);
    }
}
