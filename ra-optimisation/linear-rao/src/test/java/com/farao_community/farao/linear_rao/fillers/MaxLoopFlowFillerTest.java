/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.google.ortools.linearsolver.MPConstraint;
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

        Map<Cnec, Map<String, Double>> ptdfs = new HashMap<>();
        Map<String, Double> ptdfcnec1 = new HashMap<>();
        ptdfcnec1.put("FR", 1.0);
        ptdfcnec1.put("BE", 1.0);
        ptdfs.put(cnec1, ptdfcnec1);
        cracLoopFlowExtension.setPtdfs(ptdfs);
        Map<String, Double> netPositions = new HashMap<>();
        netPositions.put("FR", 10.0);
        netPositions.put("BE", 10.0);
        cracLoopFlowExtension.setNetPositions(netPositions);
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);

        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
        cnecLoopFlowExtension.setLoopFlowConstraint(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        maxLoopFlowFiller = new MaxLoopFlowFiller(linearRaoProblem, linearRaoData);

    }

    @Test
    public void testA() throws IOException {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);

        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(getClass().getResourceAsStream("/small-sensi-results-1.json")));
        when(linearRaoData.getSensitivityComputationResults(any())).thenReturn(sensiResults);
        coreProblemFiller.fill();

        // fill max loop flow
        maxLoopFlowFiller.fill();

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraint = linearRaoProblem.getMaxLoopFlowConstraint(cnec1);
        assertNotNull(loopFlowConstraint);
        assertEquals(-80, loopFlowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(120, loopFlowConstraint.ub(), DOUBLE_TOLERANCE);
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraint.getCoefficient(flowVariable), 0.1);
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
