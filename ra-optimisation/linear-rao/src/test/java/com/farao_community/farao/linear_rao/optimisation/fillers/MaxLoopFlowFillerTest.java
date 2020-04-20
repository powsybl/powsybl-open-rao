/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.optimisation.fillers;

import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxLoopFlowFillerTest extends AbstractFillerTest {

    private MaxLoopFlowFiller maxLoopFlowFiller;
    private GlskProvider glskProvider;
    private CracLoopFlowExtension cracLoopFlowExtension;
    private List<Country> countries;
    private ComputationManager computationManager;

    @Before
    public void setUp() throws IOException {
        init();
        coreProblemFiller = new CoreProblemFiller();
        glskProvider = glskProvider();
        cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        countries = new ArrayList<>();
        countries.add(Country.FR);
        countries.add(Country.BE);
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);

        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
        cnecLoopFlowExtension.setLoopFlowConstraint(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);

        maxLoopFlowFiller = new MaxLoopFlowFiller();
        computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
    }

    @Test
    public void testFill() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);
        coreProblemFiller.fill(situation, systematicSensitivityAnalysisResult, linearRaoProblem);

        // fill max loop flow
        maxLoopFlowFiller.fill(situation, systematicSensitivityAnalysisResult, linearRaoProblem);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraint = linearRaoProblem.getMaxLoopFlowConstraint(cnec1);
        assertNotNull(loopFlowConstraint);
        assertEquals(-100, loopFlowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(100, loopFlowConstraint.ub(), DOUBLE_TOLERANCE);
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

    static SensitivityComputationFactory sensitivityComputationFactory() {
        return new SensitivityComputationFactoryMock();
    }

    @AutoService(SensitivityComputationFactory.class)
    public static class SensitivityComputationFactoryMock implements SensitivityComputationFactory {

        public SensitivityComputationFactoryMock() {
        }

        public static <K, V> Map.Entry<K, V> entry(K key, V value) {
            return new AbstractMap.SimpleEntry<>(key, value);
        }

        public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
            return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new SensitivityComputation() {

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    List<SensitivityValue> sensitivityValues = new ArrayList<>();
                    return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValues));
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
