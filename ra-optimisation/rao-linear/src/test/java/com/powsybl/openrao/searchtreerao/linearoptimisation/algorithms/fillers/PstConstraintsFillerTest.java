/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.timecoupledconstraints.PstConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Atena Amnache {@literal <atena.amnache@rte-france.com>}
 */
class PstConstraintsFillerTest {

    List<OffsetDateTime> hourlyTimestamps;
    RaoParameters raoParameters;
    TemporalData<RaoInput> raoInputs;
    private LinearProblem linearProblem;
    private Map<RangeAction<?>, Double> initialSetpoints;
    private final LinearProblemBuilder linearProblemBuilder = new LinearProblemBuilder().withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);
    private static final double DOUBLE_EPSILON = 1e-3;

    @BeforeEach
    void setUp() {
        createTimestamps();
        initialSetpoints = new HashMap<>();
    }

    // SetUp

    private void createTimestamps() {
        hourlyTimestamps = new ArrayList<>();
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC));
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC));
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC));
    }

    private void createInitialSetpoints() {
        raoInputs.getDataPerTimestamp().values().forEach(raoInput ->
            raoInput.getCrac().getPstRangeActions().forEach(pstRangeAction ->
                initialSetpoints.put(pstRangeAction, pstRangeAction.getTapToAngleConversionMap().get(pstRangeAction.getInitialTap()))
            )
        );
    }

    private void createCoreProblemFillers() {
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Crac crac = raoInput.getCrac();
            State preventiveState = crac.getPreventiveState();
            OptimizationPerimeter optimizationPerimeter = new PreventiveOptimizationPerimeter(
                preventiveState, crac.getFlowCnecs(), Set.of(), crac.getNetworkActions(preventiveState), crac.getRangeActions(preventiveState)
            );
            RangeActionsOptimizationParameters rangeActionsOptimizationParameters = raoParameters.getRangeActionsOptimizationParameters();
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(initialSetpoints);
            // since PSTs are not costly, MarginCoreProblemFiller is used instead of CostCore
            MarginCoreProblemFiller marginCoreProblemFiller = new MarginCoreProblemFiller(
                optimizationPerimeter, rangeActionSetpointResult,
                rangeActionsOptimizationParameters, null, Unit.MEGAWATT, false,
                SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, timestamp
            );
            linearProblemBuilder.withProblemFiller(marginCoreProblemFiller);
            Map<State, Set<PstRangeAction>> pstRangeActions = Map.of(preventiveState, crac.getPstRangeActions());
            // create the tap variables
            DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(optimizationPerimeter, pstRangeActions, rangeActionSetpointResult, rangeActionsOptimizationParameters, false, false);
            linearProblemBuilder.withProblemFiller(discretePstTapFiller);
        });
    }

    private void createPstConstraintsFiller(Set<PstConstraints> pstConstraints) {
        TemporalData<State> preventiveStates = raoInputs.map(raoInput -> raoInput.getCrac().getPreventiveState());
        TemporalData<Set<PstRangeAction>> pstRangeActionsPerTimestamp = raoInputs.map(raoInput -> raoInput.getCrac().getPstRangeActions());
        PstConstraintsFiller pstConstraintsFiller = new PstConstraintsFiller(preventiveStates, pstRangeActionsPerTimestamp, pstConstraints);
        linearProblemBuilder.withProblemFiller(pstConstraintsFiller);
    }

    private void buildAndFillLinearProblem() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(any(), any(), any())).thenReturn(0.0);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        linearProblem = linearProblemBuilder
            .withInitialRangeActionActivationResult(new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(initialSetpoints)))
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void setUpLinearProblemWithPstConstraints(List<String> cracFileNames, Set<PstConstraints> pstConstraints) throws IOException {
        Network network = Network.read("TestCase12Nodes2PSTs.uct", getClass().getResourceAsStream("/network/TestCase12Nodes2PSTs.uct"));
        Map<OffsetDateTime, RaoInput> raoInputPerTimestamp = new HashMap<>();
        for (String cracFileName : cracFileNames) {
            Crac crac = Crac.read(cracFileName, getClass().getResourceAsStream("/crac/" + cracFileName), network);
            raoInputPerTimestamp.put(crac.getTimestamp().orElseThrow(), RaoInput.build(network, crac).build());
        }
        raoInputs = new TemporalDataImpl<>(raoInputPerTimestamp);
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_approximated_integers.json"), ReportNode.NO_OP);
        createInitialSetpoints();
        createCoreProblemFillers();
        createPstConstraintsFiller(pstConstraints);
        buildAndFillLinearProblem();
    }

    // Tests
    @Test
    void testPstConstrainedOnConsecutiveTimestampsOnly() throws IOException {
        PstConstraints pstConstraints = PstConstraints.create()
            .withPstId("pst_be")
            .withUpwardTapGradient(1)
            .withDownwardTapGradient(-1)
            .build();
        setUpLinearProblemWithPstConstraints(
            List.of("crac-both-psts-202601090000.json", "crac-both-psts-202601090100.json", "crac-both-psts-202601090200.json"),
            Set.of(pstConstraints)
        );
        // pst_be is optimized in the 3 timestamps :
        // one gradient constraint between the first and second,
        // one between the second and the third
        // no gradient constraint between first and third
        checkTapGradientConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(1), -1, 1);
        checkTapGradientConstraint("pst_be", hourlyTimestamps.get(1), hourlyTimestamps.get(2), -1, 1);
        checkNoTapGradientConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
    }

    @Test
    void testNoConstraintWhenPstMissingFromATimestamp() throws IOException {
        PstConstraints pstConstraints = PstConstraints.create()
            .withPstId("pst_de")
            .withUpwardTapGradient(1)
            .withDownwardTapGradient(-1)
            .build();
        setUpLinearProblemWithPstConstraints(
            List.of("crac-both-psts-202601090000.json", "crac-pst-be-only-202601090100.json", "crac-both-psts-202601090200.json"),
            Set.of(pstConstraints)
        );
        // pst_de is not optimized in the second timestamp, no gradient constraint is created for this pst
        checkNoTapGradientConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkNoTapGradientConstraint("pst_de", hourlyTimestamps.get(1), hourlyTimestamps.get(2));
        checkNoTapGradientConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
    }

    @Test
    void testTwoPstConstraints() throws IOException {
        PstConstraints pstBeConstraints = PstConstraints.create()
            .withPstId("pst_be")
            .withUpwardTapGradient(1)
            .withDownwardTapGradient(-1)
            .build();
        PstConstraints pstDeConstraints = PstConstraints.create()
            .withPstId("pst_de")
            .withUpwardTapGradient(5)
            .withDownwardTapGradient(-5)
            .build();
        setUpLinearProblemWithPstConstraints(
            List.of("crac-both-psts-202601090000.json", "crac-both-psts-202601090100.json"),
            Set.of(pstBeConstraints, pstDeConstraints)
        );
        // both tap gradient constraints are created for both PSTs
        checkTapGradientConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(1), -1, 1);
        checkTapGradientConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(1), -5, 5);
    }

    @Test
    void testSingleTimestampThrows() {
        PstConstraints pstConstraints = PstConstraints.create().withPstId("pst_be").withUpwardTapGradient(1).build();
        List<String> cracFileNames = List.of("crac-both-psts-202601090000.json");
        Set<PstConstraints> constraints = Set.of(pstConstraints);
        OpenRaoException openRaoException = assertThrows(OpenRaoException.class, () -> setUpLinearProblemWithPstConstraints(cracFileNames, constraints));
        assertEquals("There must be at least two timestamps.", openRaoException.getMessage());
    }

    // Helpers

    private void checkTapGradientConstraint(String pstId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp, double lb, double ub) {
        PstRangeAction pstRangeAction = getPstRangeAction(timestamp, pstId);
        PstRangeAction nextPstRangeAction = getPstRangeAction(nextTimestamp, pstId);
        State state = getPreventiveState(timestamp);
        State nextState = getPreventiveState(nextTimestamp);

        OpenRaoMPConstraint tapGradientConstraint = linearProblem.getPstTapGradientConstraint(pstRangeAction, state, nextState);
        assertNotNull(tapGradientConstraint);
        assertEquals(lb, tapGradientConstraint.lb(), lb == -linearProblem.infinity() ? linearProblem.infinity() * DOUBLE_EPSILON : DOUBLE_EPSILON);
        assertEquals(ub, tapGradientConstraint.ub(), ub == linearProblem.infinity() ? linearProblem.infinity() * DOUBLE_EPSILON : DOUBLE_EPSILON);

        OpenRaoMPVariable tapVariable = linearProblem.getTapVariable(pstRangeAction, state);
        OpenRaoMPVariable nextTapVariable = linearProblem.getTapVariable(nextPstRangeAction, nextState);
        assertEquals(-1.0, tapGradientConstraint.getCoefficient(tapVariable), DOUBLE_EPSILON);
        assertEquals(1.0, tapGradientConstraint.getCoefficient(nextTapVariable), DOUBLE_EPSILON);
    }

    private void checkNoTapGradientConstraint(String pstId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        PstRangeAction pstRangeAction = getPstRangeAction(timestamp, pstId);
        State state = getPreventiveState(timestamp);
        State nextState = getPreventiveState(nextTimestamp);
        PstRangeAction otherPstRangeAction = pstRangeAction != null ? pstRangeAction : getPstRangeAction(nextTimestamp, pstId);
        assertThrows(OpenRaoException.class, () -> linearProblem.getPstTapGradientConstraint(otherPstRangeAction, state, nextState));
    }

    private PstRangeAction getPstRangeAction(OffsetDateTime timestamp, String pstId) {
        return raoInputs.getData(timestamp).orElseThrow().getCrac().getPstRangeAction(pstId);
    }

    private State getPreventiveState(OffsetDateTime timestamp) {
        return raoInputs.getData(timestamp).orElseThrow().getCrac().getPreventiveState();
    }
}
