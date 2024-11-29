/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.parameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.LinearOptimizationSolver;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters raoParametersExtension = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        Crac crac = Mockito.mock(Crac.class);
        builder.withConstantParametersOverAllRao(raoParameters, crac);
        SearchTreeParameters searchTreeParameters = builder.build();
        assertNotNull(searchTreeParameters);

        assertEquals(raoParameters.getObjectiveFunctionParameters().getType(), searchTreeParameters.getObjectiveFunction());
        assertEquals(NetworkActionParameters.buildFromRaoParameters(raoParameters, crac), searchTreeParameters.getNetworkActionParameters());
        assertEquals(crac.getRaUsageLimitsPerInstant(), searchTreeParameters.getRaLimitationParameters());
        assertEquals(raoParameters.getRangeActionsOptimizationParameters(), searchTreeParameters.getRangeActionParameters());
        assertEquals(raoParameters.getMnecParameters().orElse(null), searchTreeParameters.getMnecParameters());
        assertEquals(raoParameters.getLoopFlowParameters().orElse(null), searchTreeParameters.getLoopFlowParameters());
        assertEquals(raoParametersExtension.getMnecParameters().orElse(null), searchTreeParameters.getMnecParametersExtension());
        assertEquals(raoParametersExtension.getRelativeMarginsParameters().orElse(null), searchTreeParameters.getMaxMinRelativeMarginParameters());
        assertEquals(raoParametersExtension.getLoopFlowParameters().orElse(null), searchTreeParameters.getLoopFlowParametersExtension());
        assertEquals(raoParametersExtension.getRangeActionsOptimizationParameters().getLinearOptimizationSolver(), searchTreeParameters.getSolverParameters());
        assertEquals(raoParametersExtension.getRangeActionsOptimizationParameters().getMaxMipIterations(), searchTreeParameters.getMaxNumberOfIterations());
    }

    @Test
    void testIndividualSetters() {
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction = Mockito.mock(ObjectiveFunctionParameters.ObjectiveFunctionType.class);
        Unit objectiveFunctionUnit = Mockito.mock(Unit.class);
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        Map<Instant, RaUsageLimits> raLimitationParameters = new HashMap<>();
        RangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(RangeActionsOptimizationParameters.class);
        com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters rangeActionParametersExtension = Mockito.mock(com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.class);
        MnecParameters mnecParameters = Mockito.mock(MnecParameters.class);
        RelativeMarginsParameters maxMinRelativeMarginParameters = Mockito.mock(RelativeMarginsParameters.class);
        LoopFlowParameters loopFlowParameters = Mockito.mock(LoopFlowParameters.class);
        UnoptimizedCnecParameters unoptimizedCnecParameters = Mockito.mock(UnoptimizedCnecParameters.class);
        LinearOptimizationSolver solverParameters = Mockito.mock(LinearOptimizationSolver.class);
        int maxNumberOfIterations = 3;

        SearchTreeParameters searchTreeParameters = builder
            .with0bjectiveFunction(objectiveFunction)
            .with0bjectiveFunctionUnit(objectiveFunctionUnit)
            .withTreeParameters(treeParameters)
            .withNetworkActionParameters(networkActionParameters)
            .withGlobalRemedialActionLimitationParameters(raLimitationParameters)
            .withRangeActionParameters(rangeActionParameters)
            .withRangeActionParametersExtension(rangeActionParametersExtension)
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
    void testDecreaseRemedialActionUsageLimits() throws IOException {
        Crac crac = Crac.read(
            "crac.json", SearchTreeParametersTest.class.getResourceAsStream("/crac/small-crac-with-comprehensive-usage-limits-3-curative-instants.json"),
            Network.read(Paths.get(new File(Objects.requireNonNull(SearchTreeParametersTest.class.getResource("/network/small-network-2P.uct")).getFile()).toString()))
        );

        // preventive

        SearchTreeParameters preventiveParameters = SearchTreeParameters.create()
            .withGlobalRemedialActionLimitationParameters(new HashMap<>(crac.getRaUsageLimitsPerInstant()))
            .build();

        OptimizationResult preventiveOptimizationResult = Mockito.mock(OptimizationResult.class);
        when(preventiveOptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(crac.getNetworkAction("prev-open-be-1"), crac.getNetworkAction("prev-close-be-2")));
        when(preventiveOptimizationResult.getActivatedRangeActions(crac.getPreventiveState())).thenReturn(Set.of(crac.getPstRangeAction("prev-pst-fr")));
        when(preventiveOptimizationResult.getOptimizedTap(crac.getPstRangeAction("prev-pst-fr"), crac.getPreventiveState())).thenReturn(4);

        PrePerimeterResult prePreventivePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(prePreventivePerimeterResult.getTap(crac.getPstRangeAction("prev-pst-fr"))).thenReturn(0);

        preventiveParameters.decreaseRemedialActionUsageLimits(Map.of(crac.getPreventiveState(), preventiveOptimizationResult), Map.of(crac.getPreventiveState(), prePreventivePerimeterResult));

        RaUsageLimits preventiveRaUsageLimits = preventiveParameters.getRaLimitationParameters().get(crac.getInstant("preventive"));
        assertEquals(0, preventiveRaUsageLimits.getMaxRa());
        assertEquals(Map.of("FR", 2, "BE", 1), preventiveRaUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 1), preventiveRaUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 4, "BE", 1), preventiveRaUsageLimits.getMaxRaPerTso());
        assertEquals(0, preventiveRaUsageLimits.getMaxTso());
        assertEquals(Map.of("FR", 3, "BE", 1), preventiveRaUsageLimits.getMaxElementaryActionsPerTso());

        RaUsageLimits curative1RaUsageLimitsAfterPreventiveOpt = preventiveParameters.getRaLimitationParameters().get(crac.getInstant("curative1"));
        assertEquals(2, curative1RaUsageLimitsAfterPreventiveOpt.getMaxRa());
        assertEquals(Map.of("FR", 1, "BE", 1), curative1RaUsageLimitsAfterPreventiveOpt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 2, "BE", 1), curative1RaUsageLimitsAfterPreventiveOpt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 2, "BE", 1), curative1RaUsageLimitsAfterPreventiveOpt.getMaxRaPerTso());
        assertEquals(2, curative1RaUsageLimitsAfterPreventiveOpt.getMaxTso());
        assertEquals(Map.of("FR", 1, "BE", 7), curative1RaUsageLimitsAfterPreventiveOpt.getMaxElementaryActionsPerTso());

        RaUsageLimits curative2RaUsageLimitsAfterPreventiveOpt = preventiveParameters.getRaLimitationParameters().get(crac.getInstant("curative2"));
        assertEquals(5, curative2RaUsageLimitsAfterPreventiveOpt.getMaxRa());
        assertEquals(Map.of("FR", 3, "BE", 2), curative2RaUsageLimitsAfterPreventiveOpt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 5), curative2RaUsageLimitsAfterPreventiveOpt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 6, "BE", 5), curative2RaUsageLimitsAfterPreventiveOpt.getMaxRaPerTso());
        assertEquals(3, curative2RaUsageLimitsAfterPreventiveOpt.getMaxTso());
        assertEquals(Map.of("FR", 3, "BE", 10), curative2RaUsageLimitsAfterPreventiveOpt.getMaxElementaryActionsPerTso());

        RaUsageLimits curative3RaUsageLimitsAfterPreventiveOpt = preventiveParameters.getRaLimitationParameters().get(crac.getInstant("curative3"));
        assertEquals(8, curative3RaUsageLimitsAfterPreventiveOpt.getMaxRa());
        assertEquals(Map.of("FR", 4, "BE", 5), curative3RaUsageLimitsAfterPreventiveOpt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 6, "BE", 6), curative3RaUsageLimitsAfterPreventiveOpt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 10, "BE", 8), curative3RaUsageLimitsAfterPreventiveOpt.getMaxRaPerTso());
        assertEquals(3, curative3RaUsageLimitsAfterPreventiveOpt.getMaxTso());
        assertEquals(Map.of("FR", 5, "BE", 12), curative3RaUsageLimitsAfterPreventiveOpt.getMaxElementaryActionsPerTso());

        // curative 1

        SearchTreeParameters curative1Parameters = SearchTreeParameters.create()
            .withGlobalRemedialActionLimitationParameters(new HashMap<>(crac.getRaUsageLimitsPerInstant()))
            .build();

        OptimizationResult curative1OptimizationResult = Mockito.mock(OptimizationResult.class);
        when(curative1OptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(crac.getNetworkAction("cur1-open-fr-1")));
        when(curative1OptimizationResult.getActivatedRangeActions(crac.getState("contingency", crac.getInstant("curative1")))).thenReturn(Set.of(crac.getPstRangeAction("cur1-pst-be")));
        when(curative1OptimizationResult.getOptimizedTap(crac.getPstRangeAction("cur1-pst-be"), crac.getState("contingency", crac.getInstant("curative1")))).thenReturn(-7);

        PrePerimeterResult preCurative1PerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(preCurative1PerimeterResult.getTap(crac.getPstRangeAction("cur1-pst-be"))).thenReturn(0);

        curative1Parameters.decreaseRemedialActionUsageLimits(Map.of(crac.getState("contingency", crac.getInstant("curative1")), curative1OptimizationResult), Map.of(crac.getState("contingency", crac.getInstant("curative1")), preCurative1PerimeterResult));

        RaUsageLimits curative1RaUsageLimitsAfterCurative1Opt = curative1Parameters.getRaLimitationParameters().get(crac.getInstant("curative1"));
        assertEquals(0, curative1RaUsageLimitsAfterCurative1Opt.getMaxRa());
        assertEquals(Map.of("FR", 0, "BE", 0), curative1RaUsageLimitsAfterCurative1Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimitsAfterCurative1Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimitsAfterCurative1Opt.getMaxRaPerTso());
        assertEquals(0, curative1RaUsageLimitsAfterCurative1Opt.getMaxTso());
        assertEquals(Map.of("FR", 0, "BE", 0), curative1RaUsageLimitsAfterCurative1Opt.getMaxElementaryActionsPerTso());

        // results from curative1 should impact limits for curative2 and curative3
        RaUsageLimits curative2RaUsageLimitsAfterCurative1Opt = curative1Parameters.getRaLimitationParameters().get(crac.getInstant("curative2"));
        assertEquals(3, curative2RaUsageLimitsAfterCurative1Opt.getMaxRa());
        assertEquals(Map.of("FR", 2, "BE", 2), curative2RaUsageLimitsAfterCurative1Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 4), curative2RaUsageLimitsAfterCurative1Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 5, "BE", 4), curative2RaUsageLimitsAfterCurative1Opt.getMaxRaPerTso());
        assertEquals(1, curative2RaUsageLimitsAfterCurative1Opt.getMaxTso());
        assertEquals(Map.of("FR", 2, "BE", 3), curative2RaUsageLimitsAfterCurative1Opt.getMaxElementaryActionsPerTso());

        RaUsageLimits curative3RaUsageLimitsAfterCurative1Opt = curative1Parameters.getRaLimitationParameters().get(crac.getInstant("curative3"));
        assertEquals(6, curative3RaUsageLimitsAfterCurative1Opt.getMaxRa());
        assertEquals(Map.of("FR", 3, "BE", 5), curative3RaUsageLimitsAfterCurative1Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 6, "BE", 5), curative3RaUsageLimitsAfterCurative1Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 9, "BE", 7), curative3RaUsageLimitsAfterCurative1Opt.getMaxRaPerTso());
        assertEquals(1, curative3RaUsageLimitsAfterCurative1Opt.getMaxTso());
        assertEquals(Map.of("FR", 4, "BE", 5), curative3RaUsageLimitsAfterCurative1Opt.getMaxElementaryActionsPerTso());

        // curative 2

        SearchTreeParameters curative2Parameters = SearchTreeParameters.create()
            .withGlobalRemedialActionLimitationParameters(new HashMap<>(crac.getRaUsageLimitsPerInstant()))
            .build();

        OptimizationResult curative2OptimizationResult = Mockito.mock(OptimizationResult.class);
        when(curative2OptimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(crac.getNetworkAction("cur2-open-fr-2")));
        when(curative2OptimizationResult.getActivatedRangeActions(crac.getState("contingency", crac.getInstant("curative2")))).thenReturn(Set.of());

        PrePerimeterResult preCurative2PerimeterResult = Mockito.mock(PrePerimeterResult.class);

        curative2Parameters.decreaseRemedialActionUsageLimits(Map.of(crac.getState("contingency", crac.getInstant("curative1")), curative1OptimizationResult, crac.getState("contingency", crac.getInstant("curative2")), curative2OptimizationResult), Map.of(crac.getState("contingency", crac.getInstant("curative1")), preCurative1PerimeterResult, crac.getState("contingency", crac.getInstant("curative2")), preCurative2PerimeterResult));

        RaUsageLimits curative1RaUsageLimitsAfterCurative2Opt = curative2Parameters.getRaLimitationParameters().get(crac.getInstant("curative1"));
        assertEquals(0, curative1RaUsageLimitsAfterCurative2Opt.getMaxRa());
        assertEquals(Map.of("FR", 0, "BE", 0), curative1RaUsageLimitsAfterCurative2Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimitsAfterCurative2Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 1, "BE", 0), curative1RaUsageLimitsAfterCurative2Opt.getMaxRaPerTso());
        assertEquals(0, curative1RaUsageLimitsAfterCurative2Opt.getMaxTso());
        assertEquals(Map.of("FR", 0, "BE", 0), curative1RaUsageLimitsAfterCurative2Opt.getMaxElementaryActionsPerTso());

        RaUsageLimits curative2RaUsageLimitsAfterCurative2Opt = curative2Parameters.getRaLimitationParameters().get(crac.getInstant("curative2"));
        assertEquals(2, curative2RaUsageLimitsAfterCurative2Opt.getMaxRa());
        assertEquals(Map.of("FR", 1, "BE", 2), curative2RaUsageLimitsAfterCurative2Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 4, "BE", 4), curative2RaUsageLimitsAfterCurative2Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 4, "BE", 4), curative2RaUsageLimitsAfterCurative2Opt.getMaxRaPerTso());
        assertEquals(1, curative2RaUsageLimitsAfterCurative2Opt.getMaxTso());
        assertEquals(Map.of("FR", 1, "BE", 3), curative2RaUsageLimitsAfterCurative2Opt.getMaxElementaryActionsPerTso());

        RaUsageLimits curative3RaUsageLimitsAfterCurative2Opt = curative2Parameters.getRaLimitationParameters().get(crac.getInstant("curative3"));
        assertEquals(5, curative3RaUsageLimitsAfterCurative2Opt.getMaxRa());
        assertEquals(Map.of("FR", 2, "BE", 5), curative3RaUsageLimitsAfterCurative2Opt.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 6, "BE", 5), curative3RaUsageLimitsAfterCurative2Opt.getMaxPstPerTso());
        assertEquals(Map.of("FR", 8, "BE", 7), curative3RaUsageLimitsAfterCurative2Opt.getMaxRaPerTso());
        assertEquals(1, curative3RaUsageLimitsAfterCurative2Opt.getMaxTso());
        assertEquals(Map.of("FR", 3, "BE", 5), curative3RaUsageLimitsAfterCurative2Opt.getMaxElementaryActionsPerTso());
    }
}
