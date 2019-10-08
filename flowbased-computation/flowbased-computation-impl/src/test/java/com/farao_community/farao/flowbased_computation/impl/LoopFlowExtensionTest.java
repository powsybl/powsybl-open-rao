/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowExtensionTest {
    private static final double FRM_DEFAULT_ON_FMAX = 0.1;
    private static final double RAMR_DEFAULT = 0.7;

    private GlskProvider glskProviderAll;
    private Network network;
    private CracFile cracFile;
    private GlskProvider glskProviderCore;
    private Map<String, Double> frmById;
    private Map<String, Double> ramrById;

    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    private LoopFlowExtension loopFlowExtension;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "MockLoadflow");

        network = ExampleGenerator.network();
        cracFile = ExampleGenerator.cracFile();
        glskProviderCore = ExampleGenerator.glskProvider();
        glskProviderAll = ExampleGenerator.glskProvider();
        computationManager = LocalComputationManager.getDefault();
        parameters = FlowBasedComputationParameters.load(platformConfig);

        frmById = cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, monitoredBranch -> monitoredBranch.getFmax() * FRM_DEFAULT_ON_FMAX));

        ramrById = cracFile.getPreContingency().getMonitoredBranches().stream()
                .collect(Collectors.toMap(MonitoredBranch::getBranchId, monitoredBranch -> RAMR_DEFAULT));

        LoadFlowFactory loadFlowFactory = ExampleGenerator.loadFlowFactory();
        SensitivityComputationFactory sensitivityComputationFactory = ExampleGenerator.sensitivityComputationFactory();
        LoadFlowService.init(loadFlowFactory, computationManager);
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        loopFlowExtension = new LoopFlowExtension(network, cracFile, glskProviderCore, glskProviderAll, frmById, ramrById, computationManager, parameters);
    }

    @Test
    public void testRun() {
        Map<String, Double> resultAmr = loopFlowExtension.calculateAMR();
        assertTrue(!resultAmr.isEmpty());
    }
}
