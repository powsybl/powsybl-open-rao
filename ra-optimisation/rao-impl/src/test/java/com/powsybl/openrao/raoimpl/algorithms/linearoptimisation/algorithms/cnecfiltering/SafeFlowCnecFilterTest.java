/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionImpacts;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionSetpointBounds;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
class SafeFlowCnecFilterTest {
    private static final double THRESHOLD = 2000.;
    private static final String CONTINGENCY_ID = "N-1 NL1-NL3";
    private static final MarginCriterion MAX_MIN_MARGIN = new MarginCriterion.Absolute(OptionalDouble.empty());

    private Crac crac;
    private PstRangeAction pst;
    private State preventiveState;
    private State curativeState;
    private OptimizationPerimeter perimeter;
    private RangeActionSetpointResult prePerimeterSetpoints;
    private RangeActionActivationResult linearizationPoint;
    private FlowResult flowResult;
    private SensitivityResult sensitivityResult;
    private Set<FlowCnec> optimizedCnecs;
    private RangeActionSetpointBounds.Interval pstBounds;
    private double pstMaxDistance;

    @BeforeEach
    void setUp() throws IOException {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = Crac.read("small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"), network);
        pst = crac.getPstRangeAction("PRA_PST_BE");
        preventiveState = crac.getPreventiveState();
        curativeState = crac.getState(CONTINGENCY_ID, crac.getInstant("curative"));

        optimizedCnecs = new HashSet<>();
        perimeter = Mockito.mock(OptimizationPerimeter.class);
        when(perimeter.getMainOptimizationState()).thenReturn(preventiveState);
        when(perimeter.getOptimizedFlowCnecs()).thenReturn(optimizedCnecs);
        when(perimeter.getRangeActionsPerState()).thenReturn(Map.of(preventiveState, Set.<RangeAction<?>>of(pst)));

        prePerimeterSetpoints = new RangeActionSetpointResultImpl(Map.of(pst, 0.));
        linearizationPoint = Mockito.mock(RangeActionActivationResult.class);
        when(linearizationPoint.getOptimizedSetpoint(pst, preventiveState)).thenReturn(0.);

        flowResult = Mockito.mock(FlowResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);

        // PST at tap 0 (angle 0) with an absolute range [-15, 15]: symmetric interval around the linearization point
        pstBounds = RangeActionSetpointBounds.getSetpointVariableBounds(perimeter, pst, preventiveState, prePerimeterSetpoints, Double.POSITIVE_INFINITY);
        pstMaxDistance = Math.max(pstBounds.max(), -pstBounds.min());
    }

    private FlowCnec addCnec(String id, State state, boolean optimized) {
        FlowCnec cnec = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(state.getInstant().getId())
            .withContingency(state.getContingency().map(c -> c.getId()).orElse(null))
            .withOptimized(optimized)
            .withMonitored(!optimized)
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(-THRESHOLD).withMax(THRESHOLD).add()
            .add();
        if (optimized) {
            optimizedCnecs.add(cnec);
        }
        return cnec;
    }

    /**
     * Creates an optimized CNEC whose margin at the linearization point is {@code margin} and whose margin
     * can move by at most {@code delta} through the PST.
     */
    private FlowCnec addCnec(String id, double margin, double delta) {
        FlowCnec cnec = addCnec(id, preventiveState, true);
        setMarginAndDelta(flowResult, cnec, margin, delta);
        return cnec;
    }

    private void setMarginAndDelta(FlowResult result, FlowCnec cnec, double margin, double delta) {
        setMarginAndDelta(result, sensitivityResult, cnec, TwoSides.ONE, margin, delta);
    }

    private void setMarginAndDelta(FlowResult result, SensitivityResult sensitivities, FlowCnec cnec, TwoSides side, double margin, double delta) {
        // flow >= 0, so the margin is THRESHOLD - flow
        when(result.getFlow(cnec, side, Unit.MEGAWATT)).thenReturn(Double.isNaN(margin) ? Double.NaN : THRESHOLD - margin);
        when(sensitivities.getSensitivityValue(cnec, side, pst, Unit.MEGAWATT)).thenReturn(delta / pstMaxDistance);
    }

    private FlowCnec addTwoSidedCnec(String id, double marginOne, double marginTwo, double delta) {
        FlowCnec cnec = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(preventiveState.getInstant().getId())
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(-THRESHOLD).withMax(THRESHOLD).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.TWO).withMin(-THRESHOLD).withMax(THRESHOLD).add()
            .add();
        optimizedCnecs.add(cnec);
        setMarginAndDelta(flowResult, sensitivityResult, cnec, TwoSides.ONE, marginOne, delta);
        setMarginAndDelta(flowResult, sensitivityResult, cnec, TwoSides.TWO, marginTwo, delta);
        return cnec;
    }

    /**
     * Creates an optimized CNEC with a single threshold bound on side ONE ({@code min} or {@code max} may be null).
     */
    private FlowCnec addOneBoundCnec(String id, Double min, Double max, double flow, double delta) {
        FlowCnec cnec = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(preventiveState.getInstant().getId())
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(min).withMax(max).add()
            .add();
        optimizedCnecs.add(cnec);
        when(flowResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        when(sensitivityResult.getSensitivityValue(cnec, TwoSides.ONE, pst, Unit.MEGAWATT)).thenReturn(delta / pstMaxDistance);
        return cnec;
    }

    private SafeFlowCnecFilter filter(MarginCriterion criterion) {
        return filter(criterion, Set.of(), Set.of());
    }

    private SafeFlowCnecFilter filter(MarginCriterion criterion, Set<FlowCnec> protectedCnecs, Set<FlowCnec> notWitnessCnecs) {
        RangeActionImpacts impacts = new RangeActionImpacts(perimeter, null, Unit.MEGAWATT);
        return new SafeFlowCnecFilter(perimeter, prePerimeterSetpoints, impacts, criterion, protectedCnecs, notWitnessCnecs, Unit.MEGAWATT);
    }

    private Set<String> retainIds(FlowCnecFilter filter, Set<FlowCnec> cnecs) {
        return filter.retain(cnecs, flowResult, sensitivityResult, linearizationPoint).stream().map(FlowCnec::getId).collect(Collectors.toSet());
    }

    private Set<FlowCnec> allOptimized() {
        return Set.copyOf(optimizedCnecs);
    }

    @Test
    void noneFilterReturnsInputSet() {
        FlowCnec a = addCnec("a", 10., 50.);
        Set<FlowCnec> cnecs = Set.of(a);
        assertSame(cnecs, FlowCnecFilter.NONE.retain(cnecs, flowResult, sensitivityResult, linearizationPoint));
    }

    @Test
    void farCnecIsRemovedForMaxMinMargin() {
        addCnec("far", 1000., 50.);
        addCnec("close", 10., 50.);
        addCnec("overlapping", 100., 50.);

        assertEquals(Set.of("close", "overlapping"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void overlappingCnecIsKept() {
        // low = 50 <= UB = 60
        addCnec("a", 100., 50.);
        addCnec("witness", 10., 50.);

        assertEquals(Set.of("a", "witness"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void minCostRemovesAlwaysSecureCnec() {
        // "secure" is in [200, 400], the witness in [270, 370]: UB = 370 and low = 200 <= UB
        addCnec("secure", 300., 100.);
        addCnec("witness", 320., 50.);

        assertEquals(Set.of("witness"), retainIds(filter(new MarginCriterion.Absolute(OptionalDouble.of(0.))), allOptimized()));
        assertEquals(Set.of("secure", "witness"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void allSecureKeepsWitness() {
        addCnec("a", 150., 50.);
        addCnec("b", 200., 50.);
        addCnec("c", 350., 50.);

        assertEquals(Set.of("a"), retainIds(filter(new MarginCriterion.Absolute(OptionalDouble.of(50.))), allOptimized()));
    }

    @Test
    void relativeUsesPtdfScaledComparison() {
        // absolute: a in [90, 110], b in [490, 510] -> b would be removed
        // relative: a in [900, 1100], b in [490, 510] -> a is removed
        FlowCnec a = addCnec("a", 100., 10.);
        FlowCnec b = addCnec("b", 500., 10.);
        when(flowResult.getPtdfZonalSum(a, TwoSides.ONE)).thenReturn(0.1);
        when(flowResult.getPtdfZonalSum(b, TwoSides.ONE)).thenReturn(1.0);

        assertEquals(Set.of("b"), retainIds(filter(new MarginCriterion.Relative(null, 0.02)), allOptimized()));
        assertEquals(Set.of("a"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void relativeKeepsCnecWhoseAbsoluteMarginCanBeNegative() {
        // witness: relative high = -90; negative: relative low = (-50 - 10) / 2 = -30 > -90 but absolute low is -60 < 0
        FlowCnec witness = addCnec("witness", -100., 10.);
        FlowCnec negative = addCnec("negative", -50., 10.);
        FlowCnec positive = addCnec("positive", 200., 10.);
        FlowResult ptdfSource = Mockito.mock(FlowResult.class);
        when(ptdfSource.getPtdfZonalSum(witness, TwoSides.ONE)).thenReturn(1.0);
        when(ptdfSource.getPtdfZonalSum(negative, TwoSides.ONE)).thenReturn(2.0);
        when(ptdfSource.getPtdfZonalSum(positive, TwoSides.ONE)).thenReturn(1.0);

        assertEquals(Set.of("witness", "negative"), retainIds(filter(new MarginCriterion.Relative(ptdfSource, 0.02)), allOptimized()));
    }

    @Test
    void relativeNanPtdfUsesLowerBound() {
        // a: relative low = 90 / 0.5 = 180 (NaN PTDF replaced by the lower bound), witness: high = 110
        FlowCnec a = addCnec("a", 100., 10.);
        FlowCnec witness = addCnec("witness", 100., 10.);
        when(flowResult.getPtdfZonalSum(a, TwoSides.ONE)).thenReturn(Double.NaN);
        when(flowResult.getPtdfZonalSum(witness, TwoSides.ONE)).thenReturn(1.0);

        assertEquals(Set.of("witness"), retainIds(filter(new MarginCriterion.Relative(null, 0.5)), allOptimized()));
    }

    @Test
    void failedStateCnecIsNeverWitness() {
        FlowCnec failed = addCnec("failed", curativeState, true);
        when(flowResult.getFlow(failed, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(THRESHOLD + 1000.);
        when(sensitivityResult.getSensitivityStatus(curativeState)).thenReturn(ComputationStatus.FAILURE);
        addCnec("a", 100., 10.);
        addCnec("b", 105., 10.);

        assertEquals(Set.of("failed", "a", "b"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void nanMarginIsKept() {
        addCnec("nan", Double.NaN, 10.);
        addCnec("witness", 100., 10.);
        addCnec("far", 1000., 10.);

        assertEquals(Set.of("nan", "witness"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void infiniteIntervalKeepsImpactedCnecs() {
        StandardRange relativeRange = Mockito.mock(StandardRange.class);
        when(relativeRange.getRangeType()).thenReturn(RangeType.RELATIVE_TO_PREVIOUS_INSTANT);
        when(relativeRange.getMin()).thenReturn(-10.);
        when(relativeRange.getMax()).thenReturn(10.);
        HvdcRangeAction hvdc = Mockito.mock(HvdcRangeAction.class);
        when(hvdc.getId()).thenReturn("hvdc");
        when(hvdc.getNetworkElementsIds()).thenReturn(Set.of("hvdc-element"));
        when(hvdc.getRanges()).thenReturn(List.of(relativeRange));
        Map<State, Set<RangeAction<?>>> rangeActionsPerState = new HashMap<>();
        rangeActionsPerState.put(preventiveState, Set.of(pst, hvdc));
        rangeActionsPerState.put(curativeState, Set.of(hvdc));
        when(perimeter.getRangeActionsPerState()).thenReturn(rangeActionsPerState);
        when(linearizationPoint.getOptimizedSetpoint(hvdc, curativeState)).thenReturn(0.);

        addCnec("witness", 100., 10.);
        addCnec("far", 1000., 10.);
        FlowCnec curative = addCnec("curative", curativeState, true);
        when(flowResult.getFlow(curative, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(THRESHOLD - 1000.);
        when(sensitivityResult.getSensitivityValue(curative, TwoSides.ONE, hvdc, Unit.MEGAWATT)).thenReturn(1.);

        assertEquals(Set.of("witness", "curative"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void unoptimizedCnecIsKeptAndNotWitness() {
        FlowCnec unoptimized = addCnec("unoptimized", -1000., 0.);
        addCnec("witness", 100., 10.);
        addCnec("far", 1000., 10.);

        SafeFlowCnecFilter filter = filter(MAX_MIN_MARGIN, Set.of(unoptimized), Set.of(unoptimized));
        assertEquals(Set.of("unoptimized", "witness"), retainIds(filter, allOptimized()));
    }

    @Test
    void noWitnessRemovesNothing() {
        FlowCnec a = addCnec("a", 150., 50.);
        FlowCnec b = addCnec("b", 1000., 50.);

        SafeFlowCnecFilter filter = filter(new MarginCriterion.Absolute(OptionalDouble.of(50.)), Set.of(), Set.of(a, b));
        assertEquals(Set.of("a", "b"), retainIds(filter, allOptimized()));
    }

    @Test
    void protectedCnecsArePassedThrough() {
        FlowCnec mnec = addCnec("mnec", 1000., 10.);
        FlowCnec loopFlowCnec = addCnec("loopflow", 2000., 10.);
        addCnec("witness", 100., 10.);
        addCnec("far", 1000., 10.);

        SafeFlowCnecFilter filter = filter(MAX_MIN_MARGIN, Set.of(mnec, loopFlowCnec), Set.of());
        assertEquals(Set.of("mnec", "loopflow", "witness"), retainIds(filter, allOptimized()));
    }

    @Test
    void nonCandidateCnecsArePassedThrough() {
        FlowCnec monitoredOnly = addCnec("monitored", preventiveState, false);
        when(flowResult.getFlow(monitoredOnly, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(0.);
        addCnec("witness", 100., 10.);
        addCnec("far", 1000., 10.);

        Set<FlowCnec> cnecs = new HashSet<>(optimizedCnecs);
        cnecs.add(monitoredOnly);
        assertEquals(Set.of("monitored", "witness"), retainIds(filter(MAX_MIN_MARGIN), cnecs));
    }

    @Test
    void resultIsCachedPerInputTriple() {
        FlowCnec witness = addCnec("witness", 100., 10.);
        FlowCnec far = addCnec("far", 1000., 10.);
        SafeFlowCnecFilter filter = filter(MAX_MIN_MARGIN);

        assertEquals(Set.of("witness"), retainIds(filter, allOptimized()));
        Mockito.clearInvocations(sensitivityResult);
        assertEquals(Set.of("witness"), retainIds(filter, allOptimized()));
        Mockito.verifyNoInteractions(sensitivityResult);

        // new flow result where "far" becomes the closest one: recomputed
        FlowResult otherFlowResult = Mockito.mock(FlowResult.class);
        setMarginAndDelta(otherFlowResult, witness, 1000., 10.);
        setMarginAndDelta(otherFlowResult, far, 100., 10.);
        Mockito.clearInvocations(sensitivityResult);
        Set<String> kept = filter.retain(allOptimized(), otherFlowResult, sensitivityResult, linearizationPoint).stream()
            .map(FlowCnec::getId).collect(Collectors.toSet());
        assertEquals(Set.of("far"), kept);
        Mockito.verify(sensitivityResult, Mockito.atLeastOnce()).getSensitivityStatus(any());
    }

    @Test
    void cacheIsInvalidatedByNewSensitivityResultAlone() {
        // witness in [90, 110], "far" in [115, 135]: "far" is removed
        FlowCnec witness = addCnec("witness", 100., 10.);
        FlowCnec far = addCnec("far", 125., 10.);
        SafeFlowCnecFilter filter = filter(MAX_MIN_MARGIN);
        assertEquals(Set.of("witness"), retainIds(filter, allOptimized()));

        // same flows, larger sensitivities: witness in [80, 120], "far" in [105, 145] -> overlapping, kept
        SensitivityResult otherSensitivityResult = Mockito.mock(SensitivityResult.class);
        when(otherSensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        setMarginAndDelta(flowResult, otherSensitivityResult, witness, TwoSides.ONE, 100., 20.);
        setMarginAndDelta(flowResult, otherSensitivityResult, far, TwoSides.ONE, 125., 20.);
        assertEquals(Set.of("witness", "far"), filter.retain(allOptimized(), flowResult, otherSensitivityResult, linearizationPoint).stream()
            .map(FlowCnec::getId).collect(Collectors.toSet()));
    }

    @Test
    void cacheIsInvalidatedByNewLinearizationPointAlone() {
        // witness in [90, 110], "far" in [115, 135]: "far" is removed
        addCnec("witness", 100., 10.);
        addCnec("far", 125., 10.);
        SafeFlowCnecFilter filter = filter(MAX_MIN_MARGIN);
        assertEquals(Set.of("witness"), retainIds(filter, allOptimized()));

        // linearization point on the lower bound of the PST: the maximum distance to a bound doubles, so do the deltas
        // witness in [80, 120], "far" in [105, 145] -> overlapping, kept
        RangeActionActivationResult otherLinearizationPoint = Mockito.mock(RangeActionActivationResult.class);
        when(otherLinearizationPoint.getOptimizedSetpoint(pst, preventiveState)).thenReturn(pstBounds.min());
        assertEquals(Set.of("witness", "far"), filter.retain(allOptimized(), flowResult, sensitivityResult, otherLinearizationPoint).stream()
            .map(FlowCnec::getId).collect(Collectors.toSet()));
    }

    @Test
    void twoSidedCnecUsesItsMostLimitingSide() {
        addCnec("witness", 100., 10.);
        // far on side ONE, overlapping the witness on side TWO: low = 95 <= UB = 110, kept
        addTwoSidedCnec("closeOnSideTwo", 1000., 105., 10.);
        // far on both sides: removed
        addTwoSidedCnec("farOnBothSides", 1000., 1000., 10.);

        assertEquals(Set.of("witness", "closeOnSideTwo"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void twoSidedCnecCanBeWitnessThroughOneSide() {
        addCnec("a", 100., 10.);
        // side TWO margin in [40, 60]: UB = 60, so "a" (low = 90) is removed
        addTwoSidedCnec("twoSided", 1000., 50., 10.);

        assertEquals(Set.of("twoSided"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }

    @Test
    void oneBoundCnecUsesOneSidedMargin() {
        // only a max threshold: margin = 2000 - (-5000) = 7000 (a two-bound threshold would give -3000)
        addOneBoundCnec("upperOnly", null, THRESHOLD, -5000., 10.);
        // only a min threshold: margin = -1950 - (-2000) = 50, in [40, 60] -> witness, UB = 60
        addOneBoundCnec("lowerOnly", -THRESHOLD, null, -1950., 10.);
        // in [90, 110] -> removed
        addCnec("a", 100., 10.);

        assertEquals(Set.of("lowerOnly"), retainIds(filter(MAX_MIN_MARGIN), allOptimized()));
    }
}
