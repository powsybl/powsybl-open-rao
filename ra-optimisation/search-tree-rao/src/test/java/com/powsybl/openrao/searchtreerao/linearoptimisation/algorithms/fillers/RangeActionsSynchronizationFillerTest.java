/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.commons.PstHelper;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

class RangeActionsSynchronizationFillerTest {
    private static final double DOUBLE_EPSILON = 1e-3;
    private static final String CONTINGENCY_ID = "common_contingency";  // the contingency opens FR1-FR3

    private LinearProblem linearProblem;
    private RaoParameters parameters;
    private List<OffsetDateTime> hourlyTimestamps;
    private TemporalData<RaoInput> raoInputs;
    private final LinearProblemBuilder linearProblemBuilder = new LinearProblemBuilder().withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);

    @BeforeEach
    void setUp() {
        createTimestamps();
    }

    private void createTimestamps() {
        hourlyTimestamps = new ArrayList<>();
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC));
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC));
        hourlyTimestamps.add(OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC));
    }

    private void createCoreProblemFillers() {
        raoInputs.getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Crac crac = raoInput.getCrac();
            State curativeState = getCurativeState(timestamp);
            OptimizationPerimeter optimizationPerimeter = new CurativeOptimizationPerimeter(
                    curativeState,
                    crac.getFlowCnecs(),
                    Set.of(),
                    crac.getNetworkActions(curativeState),
                    crac.getRangeActions(curativeState)
            );
            RangeActionsOptimizationParameters rangeActionsParameters = parameters.getRangeActionsOptimizationParameters();
            Map<RangeAction<?>, Double> map = new HashMap<>();
            crac.getRangeActions(curativeState).forEach(rangeAction -> map.put(rangeAction, 0.0));
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(map);
            CostCoreProblemFiller coreProblemFiller = new CostCoreProblemFiller(
                    optimizationPerimeter,
                    rangeActionSetpointResult,
                    rangeActionsParameters,
                    null,
                    Unit.MEGAWATT,
                    false,
                    SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS,
                    timestamp
            );
            linearProblemBuilder.withProblemFiller(coreProblemFiller);
        });
    }

    private void createRangeActionsSynchronizationFiller() {
        Map<OffsetDateTime, State> curativeStates = new HashMap<>();
        raoInputs.getTimestamps().forEach(timestamp -> curativeStates.put(timestamp, getCurativeState(timestamp)));
        TemporalData<State> optimizationStates = new TemporalDataImpl<>(curativeStates);
        Map<OffsetDateTime, Set<RangeAction<?>>> rangeActionsPerTimestamp = new HashMap<>();
        raoInputs.getTimestamps().forEach(timestamp ->
                rangeActionsPerTimestamp.put(timestamp, raoInputs.getData(timestamp).orElseThrow().getCrac().getRangeActions(getCurativeState(timestamp)))
        );
        TemporalData<Set<RangeAction<?>>> availableRangeActions = new TemporalDataImpl<>(rangeActionsPerTimestamp);
        RangeActionsSynchronizationFiller rangeActionsSynchronizer = new RangeActionsSynchronizationFiller(
                optimizationStates,
                availableRangeActions
        );
        linearProblemBuilder.withProblemFiller(rangeActionsSynchronizer);
    }

    private void buildAndFillLinearProblem() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(any(), any(), any())).thenReturn(0.0);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        linearProblem = linearProblemBuilder.build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void setUpLinearProblem() {
        createCoreProblemFillers();
        createRangeActionsSynchronizationFiller();
        buildAndFillLinearProblem();
    }

    private void setUpLinearProblemWithRangeActionsSynchronizationConstraints(Map<OffsetDateTime, Set<String>> pstIdsPerTimestamp) {
        Network network = Network.read("TestCase12Nodes2PSTs.uct", getClass().getResourceAsStream("/network/TestCase12Nodes2PSTs.uct"));
        Map<OffsetDateTime, RaoInput> raoInputPerTimestamp = new HashMap<>();
        pstIdsPerTimestamp.forEach((timestamp, pstIds) ->
                raoInputPerTimestamp.put(timestamp, RaoInput.build(network, createSimplePstCrac(timestamp, pstIds, network)).build())
        );
        raoInputs = new TemporalDataImpl<>(raoInputPerTimestamp);
        parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"), ReportNode.NO_OP);
        setUpLinearProblem();
    }

    /**
     * All the timestamps share both PSTs : 2 range actions shared by 3 timestamps -> 4 synchronisation constraints,
     *  all of them between the reference timestamp (the first one) and one of the other timestamps.
     */
    @Test
    void testRangeActionsSharedByAllTimestamps() {
        setUpLinearProblemWithRangeActionsSynchronizationConstraints(Map.of(
                hourlyTimestamps.get(0), Set.of("pst_be", "pst_de"),
                hourlyTimestamps.get(1), Set.of("pst_be", "pst_de"),
                hourlyTimestamps.get(2), Set.of("pst_be", "pst_de")
        ));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
        checkSynchronizationConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkSynchronizationConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
        // no constraint should be created between the two other timestamps, they are equal by transitivity
        checkNoSynchronizationConstraint("pst_be", hourlyTimestamps.get(1), hourlyTimestamps.get(2));
        checkNoSynchronizationConstraint("pst_de", hourlyTimestamps.get(1), hourlyTimestamps.get(2));
    }

    /**
     *  pst_be is shared by the three timestamps, pst_de only by the first two. Therefore, a synchronization constraint
     *  between timestamp 2 and timestamp 3 on pst_de must not be creeated for the mip.
     */
    @Test
    void testRangeActionsSharedBySomeTimestamps() {
        setUpLinearProblemWithRangeActionsSynchronizationConstraints(Map.of(
                hourlyTimestamps.get(0), Set.of("pst_be", "pst_de"),
                hourlyTimestamps.get(1), Set.of("pst_be", "pst_de"),
                hourlyTimestamps.get(2), Set.of("pst_be")
        ));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
        checkSynchronizationConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkNoSynchronizationConstraint("pst_de", hourlyTimestamps.get(1), hourlyTimestamps.get(2));
    }

    /**
     * pst_de is available in a single timestamp, it should not be tied to any other timestamp and is skipped. pst_be
     * is shared by the three timestamps and is synchronized.
     */
    @Test
    void testRangeActionAvailableInOneTimestampOnly() {
        setUpLinearProblemWithRangeActionsSynchronizationConstraints(Map.of(
                hourlyTimestamps.get(0), Set.of("pst_be", "pst_de"),
                hourlyTimestamps.get(1), Set.of("pst_be"),
                hourlyTimestamps.get(2), Set.of("pst_be")
        ));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkSynchronizationConstraint("pst_be", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
        checkNoSynchronizationConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(1));
        checkNoSynchronizationConstraint("pst_de", hourlyTimestamps.get(0), hourlyTimestamps.get(2));
    }

    private void checkSynchronizationConstraint(String pstId, OffsetDateTime referenceTimestamp, OffsetDateTime otherTimestamp) {
        State referenceState = getCurativeState(referenceTimestamp);
        State otherState = getCurativeState(otherTimestamp);
        OpenRaoMPConstraint synchronizationConstraint = linearProblem.getRangeActionSynchronizationConstraint(pstId, referenceState, otherState);
        assertNotNull(synchronizationConstraint);
        assertEquals(0.0, synchronizationConstraint.lb(), DOUBLE_EPSILON);
        assertEquals(0.0, synchronizationConstraint.ub(), DOUBLE_EPSILON);
        assertEquals(1.0, synchronizationConstraint.getCoefficient(linearProblem.getRangeActionSetpointVariable(getRangeAction(referenceTimestamp, pstId), referenceState)), DOUBLE_EPSILON);
        assertEquals(-1.0, synchronizationConstraint.getCoefficient(linearProblem.getRangeActionSetpointVariable(getRangeAction(otherTimestamp, pstId), otherState)), DOUBLE_EPSILON);
    }

    private void checkNoSynchronizationConstraint(String pstId, OffsetDateTime referenceTimestamp, OffsetDateTime otherTimestamp) {
        State referenceState = getCurativeState(referenceTimestamp);
        State otherState = getCurativeState(otherTimestamp);
        assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionSynchronizationConstraint(pstId, referenceState, otherState));
    }

    private static Crac createSimplePstCrac(OffsetDateTime timestamp, Set<String> pstIds, Network network) {
        String cracId = "crac-" + timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Crac crac = CracFactory.findDefault().create(cracId, cracId, timestamp);
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
                .newInstant("outage", InstantKind.OUTAGE)
                .newInstant("curative", InstantKind.CURATIVE);
        crac.newContingency()
                .withId(CONTINGENCY_ID)
                .withContingencyElement("FFR1AA1  FFR3AA1  1", ContingencyElementType.LINE)
                .add();
        crac.newFlowCnec()
                .withId("cnec-" + cracId)
                .withNetworkElement("FFR2AA1  FFR3AA1  1")
                .withInstant("curative")
                .withContingency(CONTINGENCY_ID)
                .withOptimized()
                .withNominalVoltage(400.0)
                .newThreshold()
                .withSide(TwoSides.ONE)
                .withMax(1000.0)
                .withMin(-1000.0)
                .withUnit(Unit.MEGAWATT)
                .add()
                .add();
        pstIds.forEach(pstId -> {
            PstHelper pstHelper = new IidmPstHelper(getNetworkElementOfPst(pstId), network);
            var pstAdder = crac.newPstRangeAction()
                    .withId(pstId)
                    .withName(pstId)
                    .withNetworkElement(getNetworkElementOfPst(pstId))
                    .withInitialTap(0)
                    .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
                    .newTapRange()
                    .withMinTap(-1)
                    .withMaxTap(1)
                    .withRangeType(RangeType.ABSOLUTE)
                    .add()
                    .newOnContingencyStateUsageRule()
                    .withContingency(CONTINGENCY_ID)
                    .withInstant("curative")
                    .add();
            pstAdder.add();
        });
        return crac;
    }

    private State getCurativeState(OffsetDateTime timestamp) {
        Crac crac = raoInputs.getData(timestamp).orElseThrow().getCrac();
        return crac.getState(CONTINGENCY_ID, crac.getInstant("curative"));
    }

    private RangeAction<?> getRangeAction(OffsetDateTime timestamp, String pstId) {
        return raoInputs.getData(timestamp).orElseThrow().getCrac().getPstRangeAction(pstId);
    }

    private static String getNetworkElementOfPst(String pstId) {
        return "pst_be".equals(pstId) ? "BBE2AA1  BBE3AA1  1" : "DDE2AA1  DDE3AA1  1";
    }
}
