/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.parameters;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SearchTreeParametersTest {
    SearchTreeParameters.SearchTreeParametersBuilder builder;

    @Before
    public void setup() {
        builder = SearchTreeParameters.create();
    }

    @Test
    public void testWithConstantParametersOverAllRao() {
        RaoParameters raoParameters = new RaoParameters();
        Crac crac = Mockito.mock(Crac.class);
        builder.withConstantParametersOverAllRao(raoParameters, crac);
        SearchTreeParameters searchTreeParameters = builder.build();
        assertNotNull(searchTreeParameters);

        assertEquals(raoParameters.getObjectiveFunctionParameters().getType(), searchTreeParameters.getObjectiveFunction());
        assertEquals(NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac), searchTreeParameters.getNetworkActionParameters());
        assertEquals(GlobalRemedialActionLimitationParameters.buildFromRaoParameters(raoParameters.getRaUsageLimitsPerContingencyParameters()), searchTreeParameters.getRaLimitationParameters());
        assertEquals(RangeActionParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getRangeActionParameters());
        assertEquals(MnecParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getMnecParameters());
        assertEquals(MaxMinRelativeMarginParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(LoopFlowParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getLoopFlowParameters());
        assertEquals(SolverParameters.buildFromRaoParameters(raoParameters), searchTreeParameters.getSolverParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), searchTreeParameters.getMaxNumberOfIterations());
    }

    @Test
    public void testIndividualSetters() {
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction = Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class);
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        GlobalRemedialActionLimitationParameters raLimitationParameters = Mockito.mock(GlobalRemedialActionLimitationParameters.class);
        RangeActionParameters rangeActionParameters = Mockito.mock(RangeActionParameters.class);
        MnecParameters mnecParameters = Mockito.mock(MnecParameters.class);
        MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = Mockito.mock(MaxMinRelativeMarginParameters.class);
        LoopFlowParameters loopFlowParameters = Mockito.mock(LoopFlowParameters.class);
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        SolverParameters solverParameters = Mockito.mock(SolverParameters.class);
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
