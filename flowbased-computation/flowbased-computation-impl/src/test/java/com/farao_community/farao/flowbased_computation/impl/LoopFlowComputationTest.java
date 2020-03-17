/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowComputationTest {
    private static final double EPSILON = 1e-3;
    private Network network;
    private Crac crac;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private List<String> countries;
    private Map<String, Double> referenceNetPositionByCountry;
    private Map<Cnec, Map<String, Double>> ptdfs;
    private Map<Cnec, Double> frefResults;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "MockLoadflow");
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        glskProvider = ExampleGenerator.glskProvider();
        computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = ExampleGenerator.sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
        countries = Arrays.asList("FR", "BE", "DE", "NL");

        ptdfs = new HashMap<>();
        frefResults = new HashMap<>();
        frefResults.put(crac.getCnec("FR-BE"), 50.0);
        frefResults.put(crac.getCnec("FR-DE"), 50.0);
        frefResults.put(crac.getCnec("BE-NL"), 50.0);
        frefResults.put(crac.getCnec("DE-NL"), 50.0);

        referenceNetPositionByCountry = new HashMap<>();
        referenceNetPositionByCountry.put("FR", 100.0);
        referenceNetPositionByCountry.put("BE", 0.0);
        referenceNetPositionByCountry.put("DE", 0.0);
        referenceNetPositionByCountry.put("NL", -100.0);
    }

    @Test
    public void testPtdf() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(network, crac, glskProvider, countries);
        ptdfs = loopFlowComputation.computePtdfOnCurrentNetwork();
        assertEquals(0.375, ptdfs.get(crac.getCnec("FR-BE")).get("FR"), EPSILON);
        assertEquals(0.375, ptdfs.get(crac.getCnec("FR-DE")).get("FR"), EPSILON);
        assertEquals(0.375, ptdfs.get(crac.getCnec("DE-NL")).get("DE"), EPSILON);
        assertEquals(0.375, ptdfs.get(crac.getCnec("BE-NL")).get("BE"), EPSILON);

        Map<String, Double> fzeroNpResults = loopFlowComputation.buildLoopFlowsFromResult(frefResults, ptdfs, referenceNetPositionByCountry);
        assertEquals(0.0, fzeroNpResults.get("FR-DE"), EPSILON);
        assertEquals(0.0, fzeroNpResults.get("FR-BE"), EPSILON);
        assertEquals(0.0, fzeroNpResults.get("DE-NL"), EPSILON);
        assertEquals(0.0, fzeroNpResults.get("BE-NL"), EPSILON);
    }
}
