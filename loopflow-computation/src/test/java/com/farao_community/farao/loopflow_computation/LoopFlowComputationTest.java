/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.util.EICode;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.flowbased_computation.glsk_provider.UcteGlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowComputationTest {
    private static final double EPSILON = 1e-3;
    private Network network;
    private Crac crac;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private List<Country> countries;
    private Map<Country, Double> referenceNetPositionByCountry;
    private Map<Cnec, Map<Country, Double>> ptdfs;
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
        countries = Stream.of("FR", "BE", "DE", "NL").map(Country::valueOf).collect(Collectors.toList());

        ptdfs = new HashMap<>();
        frefResults = new HashMap<>();
        frefResults.put(crac.getCnec("FR-BE"), 50.0);
        frefResults.put(crac.getCnec("FR-DE"), 50.0);
        frefResults.put(crac.getCnec("BE-NL"), 50.0);
        frefResults.put(crac.getCnec("DE-NL"), 50.0);

        referenceNetPositionByCountry = new HashMap<>();
        referenceNetPositionByCountry.put(Country.valueOf("FR"), 100.0);
        referenceNetPositionByCountry.put(Country.valueOf("BE"), 0.0);
        referenceNetPositionByCountry.put(Country.valueOf("DE"), 0.0);
        referenceNetPositionByCountry.put(Country.valueOf("NL"), -100.0);
    }

    @Test
    public void testConstructor() {
        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        List<Country> countriesFromGlsk = new ArrayList<>();
        glskProvider.getAllGlsk(network).keySet().forEach(key -> countriesFromGlsk.add(Country.valueOf(key)));
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);

        assertNull(crac.getExtension(CracLoopFlowExtension.class));
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        assertNotNull(loopFlowComputation);
        assertNotNull(crac.getExtension(CracLoopFlowExtension.class));

        LoopFlowComputation anotherComputation = new LoopFlowComputation(crac);
        assertNotNull(anotherComputation);
    }

    @Test
    public void testPtdf() {
        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        cracLoopFlowExtension.setGlskProvider(glskProvider);
        List<Country> countriesFromGlsk = new ArrayList<>();
        glskProvider.getAllGlsk(network).keySet().forEach(key -> countriesFromGlsk.add(Country.valueOf(key)));
        Assert.assertEquals(countriesFromGlsk.size(), countries.size());
        cracLoopFlowExtension.setCountriesForLoopFlow(countries);
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        loopFlowComputation.setCnecLoopFlowInputThresholdAsPercetangeOfPmax(0.5);
        assertEquals(50, crac.getCnec("FR-BE").getExtension(CnecLoopFlowExtension.class).getInputLoopFlow(), EPSILON);
        ptdfs = loopFlowComputation.computePtdfOnCurrentNetwork(network);
        Assert.assertEquals(0.375, ptdfs.get(crac.getCnec("FR-BE")).get(Country.valueOf("FR")), EPSILON);
        Assert.assertEquals(0.375, ptdfs.get(crac.getCnec("FR-DE")).get(Country.valueOf("FR")), EPSILON);
        Assert.assertEquals(0.375, ptdfs.get(crac.getCnec("DE-NL")).get(Country.valueOf("DE")), EPSILON);
        Assert.assertEquals(0.375, ptdfs.get(crac.getCnec("BE-NL")).get(Country.valueOf("BE")), EPSILON);
        Map<Cnec, Double> loopflowShift = loopFlowComputation.buildZeroBalanceFlowShift(ptdfs, referenceNetPositionByCountry);
        Map<String, Double> fzeroNpResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndLoopflowShifts(frefResults, loopflowShift);
        Assert.assertEquals(0.0, fzeroNpResults.get("FR-DE"), EPSILON);
        Assert.assertEquals(0.0, fzeroNpResults.get("FR-BE"), EPSILON);
        Assert.assertEquals(0.0, fzeroNpResults.get("DE-NL"), EPSILON);
        Assert.assertEquals(0.0, fzeroNpResults.get("BE-NL"), EPSILON);

        crac.getCnec("FR-BE").getExtension(CnecLoopFlowExtension.class).setLoopflowShift(1.0);
        crac.getCnec("FR-BE").getExtension(CnecLoopFlowExtension.class).setHasLoopflowShift(true);
        Map<Cnec, Double> loopflowShifts = loopFlowComputation.buildLoopflowShiftsApproximation(crac);
        assertEquals(1.0, loopflowShifts.get(crac.getCnec("FR-BE")), EPSILON);
        crac.getCnec("FR-BE").getExtension(CnecLoopFlowExtension.class).setLoopflowShift(1.0);
        crac.getCnec("FR-DE").getExtension(CnecLoopFlowExtension.class).setLoopflowShift(1.0);
        crac.getCnec("DE-NL").getExtension(CnecLoopFlowExtension.class).setLoopflowShift(1.0);
        crac.getCnec("BE-NL").getExtension(CnecLoopFlowExtension.class).setLoopflowShift(1.0);

        network.getBranch("DE-NL").getTerminal(Branch.Side.ONE).setP(10.0);
        network.getBranch("FR-BE").getTerminal(Branch.Side.ONE).setP(10.0);
        network.getBranch("FR-DE").getTerminal(Branch.Side.ONE).setP(10.0);
        network.getBranch("BE-NL").getTerminal(Branch.Side.ONE).setP(10.0);
        Map<String, Double> loopflowApprox = loopFlowComputation.calculateLoopFlowsApproximation(network);
        assertEquals(9.0, loopflowApprox.get("FR-BE"), EPSILON);
    }

    @Test
    public void testImportGlsk() {
        Network network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/glsk_lots_of_lf_12nodes.xml"), network, instant);

        List<Country> countriesFromGlsk = new ArrayList<>();
        ucteGlskProvider.getAllGlsk(network).keySet().forEach(key -> {
            countriesFromGlsk.add(new EICode(key).getCountry());
        });

        assertEquals(4, countriesFromGlsk.size());

        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, ucteGlskProvider, network);
        assertNotNull(loopFlowComputation);
    }
}
