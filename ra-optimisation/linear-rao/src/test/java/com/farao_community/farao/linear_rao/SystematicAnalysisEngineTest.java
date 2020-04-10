/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SystematicAnalysisEngineTest {
    private static final double FLOW_TOLERANCE = 1.;
    InitialSituation initialSituation;

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        crac.synchronize(network);
        initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
    }

    @Test
    public void testRun() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        RaoParameters raoParameters = RaoParameters.load(platformConfig);
        ComputationManager computationManager = LocalComputationManager.getDefault();
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        systematicAnalysisEngine.run(initialSituation);
        assertNotNull(initialSituation);
        assertEquals(512.5, initialSituation.getCost(), FLOW_TOLERANCE);

        String resultVariant = initialSituation.getResultVariant();
        assertEquals(1500., initialSituation.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
    }

    @Test
    public void testFailedRunNoFallbackParams() {
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(Mockito.mock(LinearRaoParameters.class), Mockito.mock(ComputationManager.class));
        try {
            systematicAnalysisEngine.run(initialSituation);
            fail();
        } catch (SensitivityComputationException e) {
            assertEquals("Sensitivity computation failed with default parameters. No fallback parameters available.", e.getMessage());
        }
    }
}
