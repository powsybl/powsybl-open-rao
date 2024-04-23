/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.parameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
    void testDecreaseRemedialActionUsageLimits() {
        Crac crac = CracImporters.importCrac(
            Path.of(Objects.requireNonNull(getClass().getResource("/crac/small-crac-with-comprehensive-usage-limits.json")).getFile()),
            Network.read(Objects.requireNonNull(getClass().getResource("/network/small-network-2P.uct")).getFile())
        );

        SearchTreeParameters parameters = SearchTreeParameters.create()
            .withGlobalRemedialActionLimitationParameters(
                Map.of(
                    crac.getInstant("preventive"), crac.getRaUsageLimits(crac.getInstant("preventive")),
                    crac.getInstant("curative1"), crac.getRaUsageLimits(crac.getInstant("curative1")),
                    crac.getInstant("curative2"), crac.getRaUsageLimits(crac.getInstant("curative2"))
                )
            )
            .build();

        OptimizationResult preventiveOptimizationResult = Mockito.mock(OptimizationResult.class);
        when(preventiveOptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(crac.getNetworkAction("prev-open-be-1"), crac.getNetworkAction("prev-close-be-2")));
        when(preventiveOptimizationResult.getActivatedRangeActions(crac.getPreventiveState())).thenReturn(Set.of(crac.getPstRangeAction("prev-pst-fr")));

        OptimizationResult curative1OptimizationResult = Mockito.mock(OptimizationResult.class);
        when(curative1OptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(crac.getNetworkAction("cur1-open-fr-1")));
        when(curative1OptimizationResult.getActivatedRangeActions(crac.getState("contingency", crac.getInstant("curative1")))).thenReturn(Set.of(crac.getPstRangeAction("cur1-pst-be")));

        parameters.decreaseRemedialActionUsageLimits(Map.of(crac.getPreventiveState(), preventiveOptimizationResult, crac.getState("contingency", crac.getInstant("curative1")), curative1OptimizationResult));

        RaUsageLimits preventiveRaUsageLimits = parameters.getRaLimitationParameters().get(crac.getInstant("preventive"));
        assertEquals(0, preventiveRaUsageLimits.getMaxRa());
        assertEquals(Map.of("FR", 2, "BE", 1), preventiveRaUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 1), preventiveRaUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 4, "BE", 1), preventiveRaUsageLimits.getMaxRaPerTso());
        assertEquals(0, preventiveRaUsageLimits.getMaxTso());

        RaUsageLimits curative1RaUsageLimits = parameters.getRaLimitationParameters().get(crac.getInstant("curative1"));
        assertEquals(0, curative1RaUsageLimits.getMaxRa());
        assertEquals(Map.of("FR", 0, "BE", 0), curative1RaUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimits.getMaxRaPerTso());
        assertEquals(0, curative1RaUsageLimits.getMaxTso());

        // results from curative1 should impact limits for curative2
        RaUsageLimits curative2RaUsageLimits = parameters.getRaLimitationParameters().get(crac.getInstant("curative2"));
        assertEquals(3, curative2RaUsageLimits.getMaxRa());
        assertEquals(Map.of("FR", 2, "BE", 2), curative2RaUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 4), curative2RaUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 5, "BE", 4), curative2RaUsageLimits.getMaxRaPerTso());
        assertEquals(1, curative2RaUsageLimits.getMaxTso());
    }
}
