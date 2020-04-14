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
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;
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

import static org.junit.Assert.*;

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
    private InitialSituation initialSituation;

    @Before
    public void setUp() {
        // RaoParameters
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);

        linearOptimisationEngine = Mockito.spy(new LinearOptimisationEngine(raoParameters));

        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);
        Mockito.when(linearRaoProblemMock.addMinimumMarginConstraint(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.addFlowConstraint(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.getFlowConstraint(Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.getFlowVariable(Mockito.any())).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearRaoProblemMock.getMinimumMarginVariable()).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearRaoProblemMock.getObjective()).thenReturn(Mockito.mock(MPObjective.class));
        Mockito.doReturn(linearRaoProblemMock).when(linearOptimisationEngine).createLinearRaoProblem();

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);

        initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        initialSituation = Mockito.spy(initialSituation);

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecFlowMap.put(cnec, 499.));
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap, new HashMap<>());
        Mockito.doReturn(systematicSensitivityAnalysisResult).when(initialSituation).getSystematicSensitivityAnalysisResult();
    }

    @Test
    public void testOptimalAndUpdate() {
        OptimizedSituation optimizedSituation = linearOptimisationEngine.run(initialSituation);
        assertNotNull(optimizedSituation);
        optimizedSituation = linearOptimisationEngine.run(initialSituation);
        assertNotNull(optimizedSituation);

    }

    @Test
    public void testNonOptimal() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.ABNORMAL);
        try {
            linearOptimisationEngine.run(initialSituation);
        } catch (LinearOptimisationException e) {
            assertEquals("Solving of the linear problem failed failed with MPSolver status ABNORMAL", e.getCause().getMessage());
        }
    }

    @Test
    public void testFillerError() {
        Mockito.when(linearRaoProblemMock.getObjective()).thenReturn(null);
        try {
            linearOptimisationEngine.run(initialSituation);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when building the problem.", e.getMessage());
        }
    }

    @Test
    public void testUpdateError() {
        linearOptimisationEngine.run(initialSituation);
        Mockito.when(linearRaoProblemMock.getObjective()).thenReturn(null);
        try {
            linearOptimisationEngine.run(initialSituation);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when updating the problem.", e.getMessage());
        }

    }
}
