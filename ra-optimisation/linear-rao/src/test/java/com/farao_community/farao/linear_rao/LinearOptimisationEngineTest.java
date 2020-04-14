/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LinearRaoProblem.class)
public class LinearOptimisationEngineTest {
    private LinearOptimisationEngine linearOptimisationEngine;
    private LinearRaoProblem linearRaoProblemMock;
    private Network network;
    private Crac crac;

    @Before
    public void setUp() {
        // RaoParameters
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);

        linearOptimisationEngine = Mockito.spy(new LinearOptimisationEngine(raoParameters));

        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);
        Mockito.doReturn(linearRaoProblemMock).when(linearOptimisationEngine).createLinearRaoProblem();

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);
    }

    @Test
    public void test() {
        InitialSituation initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        initialSituation = Mockito.spy(initialSituation);

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecFlowMap.put(cnec, 499.));
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap, new HashMap<>());
        Mockito.doReturn(systematicSensitivityAnalysisResult).when(initialSituation).getSystematicSensitivityAnalysisResult();
        OptimizedSituation optimizedSituation = linearOptimisationEngine.run(initialSituation);
    }
}
