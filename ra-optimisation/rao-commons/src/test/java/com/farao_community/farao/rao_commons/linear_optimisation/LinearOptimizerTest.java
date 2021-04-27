/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LinearProblem.class, LinearOptimizer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LinearOptimizerTest {
    private LinearOptimizer linearOptimizer;
    private LinearProblem linearProblemMock;
    private Network network;
    private Crac crac;
    private LinearOptimizerParameters linearOptimizerParameters;

    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        LinearOptimizerInput linearOptimizerInput = LinearOptimizerInput.create()
            .withCnecs(crac.getBranchCnecs())
            .withRangeActions(crac.getRangeActions())
            .withPreperimeterSetpoints(crac.getRangeActions().stream().collect(Collectors.toMap(
                Function.identity(),
                rangeAction -> network.getTwoWindingsTransformer(rangeAction.getNetworkElements().iterator().next().getId())
                    .getPhaseTapChanger().getCurrentStep().getAlpha()
            )))
            .build();

        linearProblemMock = Mockito.mock(LinearProblem.class);
        Mockito.when(linearProblemMock.solve()).thenReturn(LinearProblem.SolveStatus.OPTIMAL);
        Mockito.when(linearProblemMock.addMinimumMarginConstraint(anyDouble(), anyDouble(), any(), any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.addFlowConstraint(anyDouble(), anyDouble(), any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.getFlowConstraint(any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.getFlowVariable(any())).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearProblemMock.getMinimumMarginVariable()).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearProblemMock.getObjective()).thenReturn(Mockito.mock(MPObjective.class));

        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        linearOptimizer = new LinearOptimizer(linearProblemMock, linearOptimizerInput, linearOptimizerParameters);

        SystematicSensitivityResult result = createSystematicResult();
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(result, Collections.emptyMap());
    }

    private SystematicSensitivityResult createSystematicResult() {
        SystematicSensitivityResult result = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(result.isSuccess()).thenReturn(true);
        crac.getBranchCnecs().forEach(cnec -> {
            Mockito.when(result.getReferenceFlow(cnec)).thenReturn(499.);
            Mockito.when(result.getReferenceIntensity(cnec)).thenReturn(11.);
            crac.getRangeActions().forEach(rangeAction -> {
                Mockito.when(result.getSensitivityOnFlow(rangeAction, cnec)).thenReturn(42.);
                Mockito.when(result.getSensitivityOnIntensity(rangeAction, cnec)).thenReturn(-42.);
            });
        });
        return result;
    }

    @Test
    public void testOptimalAndUpdate() {
        linearOptimizer.optimize(sensitivityAndLoopflowResults);
        assertNotNull(sensitivityAndLoopflowResults);
        linearOptimizer.optimize(sensitivityAndLoopflowResults);
        assertNotNull(sensitivityAndLoopflowResults);
    }

    @Test
    public void testNonOptimal() {
        Mockito.when(linearProblemMock.solve()).thenReturn(LinearProblem.SolveStatus.ABNORMAL);
        try {
            linearOptimizer.optimize(sensitivityAndLoopflowResults);
        } catch (LinearOptimisationException e) {
            assertEquals("Solving of the linear problem failed failed with MPSolver status ABNORMAL", e.getCause().getMessage());
        }
    }

    @Test
    public void testFillerError() {
        Mockito.when(linearProblemMock.getFlowVariable(any())).thenReturn(null);
        try {
            linearOptimizer.optimize(sensitivityAndLoopflowResults);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when building the problem.", e.getMessage());
        }
    }

    @Test
    public void testUpdateError() {
        linearOptimizer.optimize(sensitivityAndLoopflowResults);
        Mockito.when(linearProblemMock.getFlowConstraint(any())).thenReturn(null);
        try {
            linearOptimizer.optimize(sensitivityAndLoopflowResults);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when updating the problem.", e.getMessage());
        }
    }
}
