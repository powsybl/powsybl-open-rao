/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoTopoOptimizationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoParametersIoTest {
    @Test
    void testRoundTripOnParameters() {
        RaoParameters raoParameters = new RaoParameters();

        // make some parameters different from the default ones to ensure import/export works
        ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
        objectiveFunctionParameters.setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        objectiveFunctionParameters.setUnit(Unit.AMPERE);
        raoParameters.setObjectiveFunctionParameters(objectiveFunctionParameters);

        // add extension
        OpenRaoSearchTreeParameters searchTreeParameters = new OpenRaoSearchTreeParameters();
        SearchTreeRaoTopoOptimizationParameters topoOptimizationParameters = new SearchTreeRaoTopoOptimizationParameters();
        topoOptimizationParameters.setMaxCurativeSearchTreeDepth(100);
        searchTreeParameters.setTopoOptimizationParameters(topoOptimizationParameters);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParameters);

        RaoParameters importedParameters = exportAndImportParameters(raoParameters);
        assertEquals(Unit.AMPERE, importedParameters.getObjectiveFunctionParameters().getUnit());
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST, importedParameters.getObjectiveFunctionParameters().getType());
        assertTrue(importedParameters.hasExtension(OpenRaoSearchTreeParameters.class));
        assertEquals(100, importedParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth());
    }

    private static RaoParameters exportAndImportParameters(RaoParameters raoParameters) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        raoParameters.write(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return RaoParameters.read(inputStream);
    }
}
