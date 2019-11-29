/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition.json;

import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.InjectionStrategy;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.PstStrategy;
import com.farao_community.farao.flow_decomposition.json.JsonFlowDecompositionParameters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFullLineDecompositionParametersTest {
    @Test
    public void read() {
        FlowDecompositionParameters fdParams = JsonFlowDecompositionParameters.read(getClass().getResourceAsStream("/params_flow_decomposition.json"));
        FullLineDecompositionParameters params = fdParams.getExtension(FullLineDecompositionParameters.class);
        assertNotNull(params);

        assertEquals(InjectionStrategy.DECOMPOSE_INJECTIONS, params.getInjectionStrategy());
        assertEquals(1e-2, params.getPexMatrixTolerance(), 0.);
        assertEquals(10, params.getThreadsNumber());
        assertEquals(PstStrategy.NEUTRAL_TAP, params.getPstStrategy());
    }

    private static void assertParametersEqual(FullLineDecompositionParameters p1, FullLineDecompositionParameters p2) {
        assertEquals(p1.getInjectionStrategy(), p2.getInjectionStrategy());
        assertEquals(p1.getPexMatrixTolerance(), p2.getPexMatrixTolerance(), 0.);
        assertEquals(p1.getThreadsNumber(), p2.getThreadsNumber());
        assertEquals(p1.getPstStrategy(), p2.getPstStrategy());
    }

    @Test
    public void roundTrip() {
        FlowDecompositionParameters lfParams = new FlowDecompositionParameters();
        FullLineDecompositionParameters params = new FullLineDecompositionParameters();
        lfParams.addExtension(FullLineDecompositionParameters.class, params);

        params.setInjectionStrategy(InjectionStrategy.DECOMPOSE_INJECTIONS)
                .setPexMatrixTolerance(1e-2)
                .setThreadsNumber(10)
                .setPstStrategy(PstStrategy.NEUTRAL_TAP);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonFlowDecompositionParameters.write(lfParams, out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FlowDecompositionParameters lfParams2 = JsonFlowDecompositionParameters.read(in);
        assertNotNull(lfParams2);

        FullLineDecompositionParameters params2 = lfParams2.getExtension(FullLineDecompositionParameters.class);
        assertNotNull(params2);

        assertParametersEqual(params, params2);
    }
}
