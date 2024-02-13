/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.parameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SearchTreeParametersTest {
    SearchTreeParameters.SearchTreeParametersBuilder builder;

    @BeforeEach
    public void setup() {
        builder = SearchTreeParameters.create();
    }

    @Test
    void testWithConstantParametersOverAllRao() {
        RaoParameters raoParameters = new RaoParameters();
        Crac crac = Mockito.mock(Crac.class);
        builder.withConstantParametersOverAllRao(raoParameters, crac);
        SearchTreeParameters searchTreeParameters = builder.build();
        assertNotNull(searchTreeParameters);

        assertEquals(raoParameters.getObjectiveFunctionParameters().getType(), searchTreeParameters.getObjectiveFunction());
        assertEquals(NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac), searchTreeParameters.getNetworkActionParameters());
        assertEquals(crac.getRaUsageLimitsPerInstant(), searchTreeParameters.getRaLimitationParameters());
        assertEquals(RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getRangeActionParameters());
        assertEquals(raoParameters.getExtension(MnecParametersExtension.class), searchTreeParameters.getMnecParameters());
        assertEquals(raoParameters.getExtension(RelativeMarginsParametersExtension.class), searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(raoParameters.getExtension(LoopFlowParametersExtension.class), searchTreeParameters.getLoopFlowParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver(), searchTreeParameters.getSolverParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), searchTreeParameters.getMaxNumberOfIterations());
    }

    @Test
    void testIndividualSetters() {
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction = Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class);
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        Map<String, RaUsageLimits> raLimitationParameters = new HashMap<>();
        RangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(RangeActionsOptimizationParameters.class);
        MnecParametersExtension mnecParameters = Mockito.mock(MnecParametersExtension.class);
        RelativeMarginsParametersExtension maxMinRelativeMarginParameters = Mockito.mock(RelativeMarginsParametersExtension.class);
        LoopFlowParametersExtension loopFlowParameters = Mockito.mock(LoopFlowParametersExtension.class);
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters = Mockito.mock(RangeActionsOptimizationParameters.LinearOptimizationSolver.class);
        int maxNumberOfIterations = 3;

        SearchTreeParameters searchTreeParameters = builder
            .with0bjectiveFunction(objectiveFunction)
            .withTreeParameters(treeParameters)
            .withNetworkActionParameters(networkActionParameters)
            .withGlobalRemedialActionLimitationParameters(raLimitationParameters)
            .withRangeActionParameters(rangeActionParameters)
            .withMnecParameters(mnecParameters)
            .withMaxMinRelativeMarginParameters(maxMinRelativeMarginParameters)
            .withLoopFlowParameters(loopFlowParameters)
            .withUnoptimizedCnecParameters(unoptimizedCnecParameters)
            .withSolverParameters(solverParameters)
            .withMaxNumberOfIterations(maxNumberOfIterations)
            .build();

        assertEquals(objectiveFunction, searchTreeParameters.getObjectiveFunction());
        assertEquals(treeParameters, searchTreeParameters.getTreeParameters());
        assertEquals(networkActionParameters, searchTreeParameters.getNetworkActionParameters());
        assertEquals(raLimitationParameters, searchTreeParameters.getRaLimitationParameters());
        assertEquals(rangeActionParameters, searchTreeParameters.getRangeActionParameters());
        assertEquals(mnecParameters, searchTreeParameters.getMnecParameters());
        assertEquals(maxMinRelativeMarginParameters, searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(loopFlowParameters, searchTreeParameters.getLoopFlowParameters());
        assertEquals(unoptimizedCnecParameters, searchTreeParameters.getUnoptimizedCnecParameters());
        assertEquals(solverParameters, searchTreeParameters.getSolverParameters());
        assertEquals(maxNumberOfIterations, searchTreeParameters.getMaxNumberOfIterations());
    }
}
