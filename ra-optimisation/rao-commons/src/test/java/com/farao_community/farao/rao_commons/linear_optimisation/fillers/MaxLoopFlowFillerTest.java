/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxLoopFlowFillerTest extends AbstractFillerTest {

    private MaxLoopFlowFiller maxLoopFlowFiller;
    private CracLoopFlowExtension cracLoopFlowExtension;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        GlskProvider glskProvider = glskProvider();
        cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        List<Country> countries = new ArrayList<>();
        countries.add(Country.FR);
        countries.add(Country.BE);
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);

        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
        cnecLoopFlowExtension.setLoopFlowConstraint(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        maxLoopFlowFiller = new MaxLoopFlowFiller();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
    }

    @Test
    public void testFill() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);
        coreProblemFiller.fill(raoData, linearProblem);

        // fill max loop flow
        maxLoopFlowFiller.setLoopFlowApproximation(false);
        maxLoopFlowFiller.fill(raoData, linearProblem);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraint = linearProblem.getMaxLoopFlowConstraint(cnec1);
        assertNotNull(loopFlowConstraint);
        assertEquals(-100, loopFlowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(100, loopFlowConstraint.ub(), DOUBLE_TOLERANCE);
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraint.getCoefficient(flowVariable), 0.1);
    }

    @Test
    public void testFillLoopflow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);
        coreProblemFiller.fill(raoData, linearProblem);

        // fill max loop flow
        maxLoopFlowFiller.setLoopFlowApproximation(true);
        maxLoopFlowFiller.fill(raoData, linearProblem);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraint = linearProblem.getMaxLoopFlowConstraint(cnec1);
        assertNotNull(loopFlowConstraint);
        assertEquals(-100, loopFlowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(100, loopFlowConstraint.ub(), DOUBLE_TOLERANCE);
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraint.getCoefficient(flowVariable), 0.1);
    }

    private static GlskProvider glskProvider() {
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

    private static SensitivityComputationFactory sensitivityComputationFactory() {
        return new SensitivityComputationFactoryMock();
    }

    @AutoService(SensitivityComputationFactory.class)
    public static class SensitivityComputationFactoryMock implements SensitivityComputationFactory {

        public SensitivityComputationFactoryMock() {
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new SensitivityComputation() {

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", Collections.emptyList()));
                }

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, ContingenciesProvider contingenciesProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", Collections.emptyList(), Collections.emptyMap()));
                }

                @Override
                public String getName() {
                    return "MockSensitivity";
                }

                @Override
                public String getVersion() {
                    return "1.0.0";
                }
            };
        }
    }
}
