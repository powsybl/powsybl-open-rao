/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.sensitivity.json.SensitivityComputationResultJsonSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxLoopFlowFillerTest extends AbstractFillerTest {
    private MaxLoopFlowFiller maxLoopFlowFiller;
    private GlskProvider glskProvider;
    private CracLoopFlowExtension cracLoopFlowExtension;
    private List<String> countries;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData);
        glskProvider = glskProvider();
        cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        countries = new ArrayList<>();
        countries.add("FR");
        countries.add("BE");
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);
        maxLoopFlowFiller = new MaxLoopFlowFiller(linearRaoProblem, linearRaoData);
    }

    @Test
    public void testA() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);

    }

    @Test
    public void test() throws IOException {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(getClass().getResourceAsStream("/small-sensi-results-1.json")));

        // complete the mock of linearRaoData
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT1);
        when(linearRaoData.getSensitivityComputationResults(any())).thenReturn(sensiResults);

        // fill the problem
        coreProblemFiller.fill();

        // check flow variable for cnec1
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);
    }

    static GlskProvider glskProvider() {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR", new LinearGlsk("FR", "FR", Collections.singletonMap("GENERATOR_FR_1", 1.f)));
        glsks.put("BE", new LinearGlsk("BE", "BE", Collections.singletonMap("GENERATOR_BE_1.1", 1.f)));
        return new GlskProvider() {
            @Override
            public Map<String, LinearGlsk> getAllGlsk(Network network) {
                return glsks;
            }

            @Override
            public LinearGlsk getGlsk(Network network, String area) {
                return glsks.get(area);
            }
        };
    }

}
