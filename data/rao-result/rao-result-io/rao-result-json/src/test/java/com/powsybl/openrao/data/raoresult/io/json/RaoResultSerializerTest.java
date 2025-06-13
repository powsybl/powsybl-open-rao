/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.utils.ExhaustiveRaoResultCreation;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.STATE_COMPARATOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class RaoResultSerializerTest {

    @Test
    void testSerialize() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithFastRaoExtension() throws IOException {
        FastRaoResultImpl fastRaoResult = Mockito.mock(FastRaoResultImpl.class);
        Crac crac = ExhaustiveCracCreation.create();

        when(fastRaoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);

        List<State> sortedListOfStates = crac.getStates().stream()
            .filter(state -> !crac.getFlowCnecs(state).isEmpty())
            .sorted(STATE_COMPARATOR)
            .toList();
        for (State state : sortedListOfStates) {
            when(fastRaoResult.getComputationStatus(state)).thenReturn(ComputationStatus.FAILURE);
        }

        when(fastRaoResult.getFinalConsideredCnecs()).thenReturn(crac.getFlowCnecs());

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(fastRaoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result-fastrao-extension.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

}
