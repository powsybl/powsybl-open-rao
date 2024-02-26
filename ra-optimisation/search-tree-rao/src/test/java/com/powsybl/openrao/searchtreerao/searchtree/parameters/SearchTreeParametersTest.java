/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.parameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.CracImpl;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SearchTreeParametersTest {
    SearchTreeParameters.SearchTreeParametersBuilder builder;
    private Crac crac;

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
        Map<Instant, RaUsageLimits> raLimitationParameters = new HashMap<>();
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

    @Test
    void testRaLimitsSetter() {
        // Set up
        Map<Instant, RaUsageLimits> raLimitationParameters = new HashMap<>();
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxRa(3);
        raUsageLimits.setMaxTso(2);
        Map<String, Integer> raLimitsPerTso = new HashMap<>();
        raLimitsPerTso.put("BE", 10);
        raLimitsPerTso.put("FR", 3);
        raUsageLimits.setMaxRaPerTso(raLimitsPerTso);
        Map<String, Integer> pstLimitsPerTso = new HashMap<>();
        pstLimitsPerTso.put("BE", 10);
        pstLimitsPerTso.put("FR", 1);
        raUsageLimits.setMaxPstPerTso(pstLimitsPerTso);
        Map<String, Integer> topoLimitsPerTso = new HashMap<>();
        topoLimitsPerTso.put("BE", 10);
        topoLimitsPerTso.put("FR", 2);
        raUsageLimits.setMaxTopoPerTso(topoLimitsPerTso);
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveInstant.getId()).thenReturn("preventive");
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(curativeInstant.getId()).thenReturn("curative");
        raLimitationParameters.put(preventiveInstant, raUsageLimits);
        raLimitationParameters.put(curativeInstant, new RaUsageLimits());
        SearchTreeParameters searchTreeParameters = builder.withGlobalRemedialActionLimitationParameters(raLimitationParameters).build();
        RangeAction<?> ra1 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra2 = Mockito.mock(RangeAction.class);
        when(ra1.getOperator()).thenReturn("FR");
        when(ra2.getOperator()).thenReturn("FR");
        // assertions
        searchTreeParameters.setRaLimitationsForSecondPreventive(searchTreeParameters.getRaLimitationParameters().get(preventiveInstant), Set.of(ra1, ra2), preventiveInstant);
        Map<Instant, RaUsageLimits> updatedMap = searchTreeParameters.getRaLimitationParameters();
        assertEquals(2, updatedMap.keySet().size());
        assertEquals(new RaUsageLimits(), updatedMap.get(curativeInstant));
        RaUsageLimits updatedRaUsageLimits = updatedMap.get(preventiveInstant);
        assertEquals(1, updatedRaUsageLimits.getMaxRa());
        assertEquals(1, updatedRaUsageLimits.getMaxTso());
        Map<String, Integer> maxRaPerTso = updatedRaUsageLimits.getMaxRaPerTso();
        assertEquals(10, maxRaPerTso.get("BE"));
        assertEquals(1, maxRaPerTso.get("FR"));
        assertEquals(maxRaPerTso, updatedRaUsageLimits.getMaxTopoPerTso());
        Map<String, Integer> maxPstPerTso = updatedRaUsageLimits.getMaxPstPerTso();
        assertEquals(10, maxPstPerTso.get("BE"));
        assertEquals(0, maxPstPerTso.get("FR"));
    }

    @Test
    void decreaseNumberOfApplicableRemedialActions() {
        setUpCrac();
        SearchTreeParameters parameters;
        Map<Instant, Integer> appliedRemedialActions;

        // Preventive
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("preventive"), 3);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 4, 1, 3, 7);

        // Curative 1 only
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative1"), 1);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 0, 2, 6);

        // Curative 2 only
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative2"), 2);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 1, 1, 5);

        // Curative 3 only
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative3"), 6);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 1, 3, 1);

        // Curative 1 + 2
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("preventive"), 5, crac.getInstant("curative1"), 1, crac.getInstant("curative2"), 2);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 2, 0, 0, 4);

        // Curative 1 + 3
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative1"), 1, crac.getInstant("curative3"), 4);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 0, 2, 2);

        // Curative 2 + 3
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative2"), 2, crac.getInstant("curative3"), 1);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 1, 1, 4);

        // All curatives
        parameters = setUpParameters();
        appliedRemedialActions = Map.of(crac.getInstant("curative1"), 1, crac.getInstant("curative2"), 2, crac.getInstant("curative3"), 4);
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 0, 0, 0);

        // None
        parameters = setUpParameters();
        appliedRemedialActions = Map.of();
        parameters.decreaseNumberOfApplicableRemedialActions(appliedRemedialActions);
        assertMaxRaForAllInstants(parameters, 7, 1, 3, 7);
    }

    private void assertMaxRaForAllInstants(SearchTreeParameters parameters, int preventiveRas, int curative1Ras, int curative2Ras, int curative3Ras) {
        assertEquals(preventiveRas, parameters.getRaLimitationParameters().get(crac.getInstant("preventive")).getMaxRa());
        assertEquals(curative1Ras, parameters.getRaLimitationParameters().get(crac.getInstant("curative1")).getMaxRa());
        assertEquals(curative2Ras, parameters.getRaLimitationParameters().get(crac.getInstant("curative2")).getMaxRa());
        assertEquals(curative3Ras, parameters.getRaLimitationParameters().get(crac.getInstant("curative3")).getMaxRa());
    }

    private void setUpCrac() {
        crac = new CracImpl("dummy-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE)
            .newInstant("curative3", InstantKind.CURATIVE);
    }

    private SearchTreeParameters setUpParameters() {
        RaUsageLimits preventiveRaUsageLimits = new RaUsageLimits();
        preventiveRaUsageLimits.setMaxRa(7);
        RaUsageLimits curative1RaUsageLimits = new RaUsageLimits();
        curative1RaUsageLimits.setMaxRa(1);
        RaUsageLimits curative2RaUsageLimits = new RaUsageLimits();
        curative2RaUsageLimits.setMaxRa(3);
        RaUsageLimits curative3RaUsageLimits = new RaUsageLimits();
        curative3RaUsageLimits.setMaxRa(7);

        return SearchTreeParameters.create()
            .withGlobalRemedialActionLimitationParameters(
                Map.of(
                    crac.getInstant("preventive"), preventiveRaUsageLimits,
                    crac.getInstant("curative1"), curative1RaUsageLimits,
                    crac.getInstant("curative2"), curative2RaUsageLimits,
                    crac.getInstant("curative3"), curative3RaUsageLimits
                )
            )
            .build();
    }
}
