/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.*;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
class MultiTSFillerTest {
    static final double DOUBLE_TOLERANCE = 1e-4;
    static final double SENSITIVITY_THRESHOLD = 1e-6;
    // data related to the Range Action
    static final int TAP_INITIAL = 1;
    static final int TAP_BETWEEN_MIP = 5;
    static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";

    static final double SENSI_CNEC = 25.0;
    static final double REF_FLOW_CNEC = -500;

    List<FlowCnec> cnecs;
    List<PstRangeAction> pstRangeActions;
    FlowResult flowResult;
    SensitivityResult sensitivityResult;
    List<Crac> cracs;
    List<Network> networks;
    RaoParameters raoParameters;
    private LinearProblem linearProblem;
    private List<CoreProblemFiller> coreProblemFillers;
    private List<DiscretePstTapFiller> discretePstTapFillers;
    private MultiTSFiller multiTSFiller;
    private RangeActionSetpointResult initialRangeActionSetpointResult;
    private int tapRelativeToTsMin;
    private int tapRelativeToTsMax;
    private double setPointRelativeToTsMin;
    private double setPointRelativeToTsMax;

    private double initialAlpha;
    private RangeActionsOptimizationParameters rangeActionParameters;
    private List<OptimizationPerimeter> optimizationPerimeters;

    void initTwoTimesteps() {
        // arrange some data for all fillers test
        // crac and network
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case1_0.json",
            "multi-ts/crac/crac-case1_1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR.uct",
            "multi-ts/network/12NodesProdFR.uct"
        );
        importNetworksAndCracs(cracsPaths, networksPaths);
        initCnecsAndRangeActions();

        networks.get(0).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        networks.get(1).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);

        tapRelativeToTsMin = pstRangeActions.get(1).getRanges().get(1).getMinTap();
        tapRelativeToTsMax = pstRangeActions.get(1).getRanges().get(1).getMaxTap();
        setPointRelativeToTsMin = pstRangeActions.get(1).convertTapToAngle(tapRelativeToTsMin);
        setPointRelativeToTsMax = pstRangeActions.get(1).convertTapToAngle(tapRelativeToTsMax);

        initialAlpha = pstRangeActions.get(0).convertTapToAngle(networks.get(0).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
    }

    void initFourTimesteps() {
        // arrange some data for all fillers test
        // crac and network
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case1_0.json",
            "multi-ts/crac/crac-no-ra-1.json",
            "multi-ts/crac/crac-no-ra-2.json",
            "multi-ts/crac/crac-pst-3.json"
        );
        List<String> networksPaths = Collections.nCopies(4, "multi-ts/network/12NodesProdFR.uct");

        importNetworksAndCracs(cracsPaths, networksPaths);

        // get cnecs and rangeActions
        cnecs = new ArrayList<>();
        pstRangeActions = new ArrayList<>();
        Map<RangeAction<?>, Double> setPointsMap = new HashMap<>();
        optimizationPerimeters = new ArrayList<>();
        for (int timeStepIndex = 0; timeStepIndex < cracs.size(); timeStepIndex++) {
            Crac crac = cracs.get(timeStepIndex);
            FlowCnec cnec = crac.getFlowCnecs().iterator().next();
            cnecs.add(cnec);
            OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
            Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec));
            Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(crac.getPreventiveState());

            if (timeStepIndex == 0 || timeStepIndex == 3) {
                PstRangeAction pstRangeAction = crac.getPstRangeActions().iterator().next();
                pstRangeActions.add(pstRangeAction);
                setPointsMap.put(pstRangeAction, pstRangeAction.convertTapToAngle(pstRangeAction.getInitialTap()));
                Map<State, Set<RangeAction<?>>> rangeActions = Map.of(cnec.getState(), Set.of(pstRangeAction));
                Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);
                Mockito.when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(pstRangeAction));
            }
            optimizationPerimeters.add(optimizationPerimeter);
        }
        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(setPointsMap);

        flowResult = Mockito.mock(FlowResult.class);
        for (FlowCnec cnec : cnecs) {
            when(flowResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC);
        }
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityValue(cnecs.get(0), Side.LEFT, pstRangeActions.get(0), Unit.MEGAWATT)).thenReturn(SENSI_CNEC);
        when(sensitivityResult.getSensitivityValue(cnecs.get(1), Side.LEFT, pstRangeActions.get(0), Unit.MEGAWATT)).thenReturn(SENSI_CNEC);
        when(sensitivityResult.getSensitivityValue(cnecs.get(2), Side.LEFT, pstRangeActions.get(0), Unit.MEGAWATT)).thenReturn(SENSI_CNEC);
        when(sensitivityResult.getSensitivityValue(cnecs.get(3), Side.LEFT, pstRangeActions.get(1), Unit.MEGAWATT)).thenReturn(SENSI_CNEC);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);

        networks.get(0).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        networks.get(1).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);

        tapRelativeToTsMin = pstRangeActions.get(1).getRanges().get(1).getMinTap();
        tapRelativeToTsMax = pstRangeActions.get(1).getRanges().get(1).getMaxTap();
        setPointRelativeToTsMin = pstRangeActions.get(1).convertTapToAngle(tapRelativeToTsMin);
        setPointRelativeToTsMax = pstRangeActions.get(1).convertTapToAngle(tapRelativeToTsMax);

        initialAlpha = pstRangeActions.get(0).convertTapToAngle(networks.get(0).getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
    }

    private void importNetworksAndCracs(List<String> cracsPaths, List<String> networksPaths) {
        cracs = new ArrayList<>();
        networks = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }
    }

    private void initCnecsAndRangeActions() {
        // get cnecs and rangeActions
        cnecs = new ArrayList<>();
        pstRangeActions = new ArrayList<>();
        Map<RangeAction<?>, Double> setPointsMap = new HashMap<>();
        optimizationPerimeters = new ArrayList<>();
        for (Crac crac : cracs) {

            PstRangeAction pstRangeAction = crac.getPstRangeActions().iterator().next();
            pstRangeActions.add(pstRangeAction);
            FlowCnec cnec = crac.getFlowCnecs().iterator().next();
            cnecs.add(cnec);

            setPointsMap.put(pstRangeAction, pstRangeAction.convertTapToAngle(pstRangeAction.getInitialTap()));

            OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
            Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec));
            Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(crac.getPreventiveState());
            Map<State, Set<RangeAction<?>>> rangeActions = Map.of(cnec.getState(), Set.of(pstRangeAction));
            Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);
            Mockito.when(optimizationPerimeter.getRangeActions()).thenReturn(Set.of(pstRangeAction));
            optimizationPerimeters.add(optimizationPerimeter);
        }

        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(setPointsMap);

        flowResult = Mockito.mock(FlowResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
    }

    @BeforeEach
    public void setUp() {
        raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(SENSITIVITY_THRESHOLD);
        rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
    }

    private void buildLinearProblem() {
        coreProblemFillers = new ArrayList<>();
        discretePstTapFillers = new ArrayList<>();
        for (int i = 0; i < cracs.size(); i++) {
            initializeCoreProblemFiller(i);
        }
        initializeMultiTSFiller();
        LinearProblemBuilderMultiTS linearProblemBuilder = new LinearProblemBuilderMultiTS();
        for (CoreProblemFiller coreProblemFiller : coreProblemFillers) {
            linearProblemBuilder.withProblemFiller(coreProblemFiller);
        }
        if (raoParameters.getRangeActionsOptimizationParameters().getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            for (int i = 0; i < cracs.size(); i++) {
                initializeDiscretePstTapFiller(i);
                linearProblemBuilder.withProblemFiller(discretePstTapFillers.get(i));
            }
        }
        Set<FlowCnec> allCnecs = new HashSet<>(cnecs);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(allCnecs, Unit.MEGAWATT);
        linearProblemBuilder.withProblemFiller(multiTSFiller)
            .withProblemFiller(maxMinMarginFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP);
        linearProblem = linearProblemBuilder.build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void initializeContinuous() {
        initTwoTimesteps();

        raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        buildLinearProblem();
    }

    private void initializeDiscrete() {
        initTwoTimesteps();

        raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        buildLinearProblem();
    }

    private void initializeCoreProblemFiller(int timeStepIndex) {
        coreProblemFillers.add(
            new CoreProblemFiller(
                optimizationPerimeters.get(timeStepIndex),
                initialRangeActionSetpointResult,
                new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
                rangeActionParameters,
                Unit.MEGAWATT, false,
                timeStepIndex)
        );
    }

    private void initializeDiscretePstTapFiller(int timeStepIndex) {
        Map<State, Set<PstRangeAction>> rangeActions = new HashMap<>();
        rangeActions.put(cnecs.get(timeStepIndex).getState(), Set.of(pstRangeActions.get(timeStepIndex)));
        discretePstTapFillers.add(
            new DiscretePstTapFiller(
                networks.get(timeStepIndex),
                optimizationPerimeters.get(timeStepIndex),
                rangeActions,
                initialRangeActionSetpointResult)
        );
    }

    private void initializeMultiTSFiller() {
        multiTSFiller = new MultiTSFiller(
            optimizationPerimeters,
            networks,
            rangeActionParameters,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult)
        );
    }

    private void updateMipAndChangeResult() {
        // update the problem
        PstRangeAction pstRangeAction0 = pstRangeActions.get(0);
        PstRangeAction pstRangeAction1 = pstRangeActions.get(1);
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction0, pstRangeAction0.convertTapToAngle(pstRangeAction0.getInitialTap()),
            pstRangeAction1, pstRangeAction1.convertTapToAngle(TAP_BETWEEN_MIP)
        ));
        linearProblem.updateBetweenMipIteration(new RangeActionActivationResultImpl(rangeActionSetpointResult));
    }

    @Test
    void testFillPstContinuous() {
        initializeContinuous();
        State state0 = cnecs.get(0).getState();
        State state1 = cnecs.get(1).getState();
        PstRangeAction pstRangeAction0 = pstRangeActions.get(0);
        PstRangeAction pstRangeAction1 = pstRangeActions.get(1);

        OpenRaoMPVariable setPointVariable0 = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state0);
        OpenRaoMPVariable setPointVariable1 = linearProblem.getRangeActionSetpointVariable(pstRangeAction1, state1);

        // check constraint relative to timestamp
        OpenRaoMPConstraint relativeSetPointConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction1, state1, LinearProblem.RaRangeShrinking.FALSE);
        assertNotNull(relativeSetPointConstraint);
        assertEquals(setPointRelativeToTsMin, relativeSetPointConstraint.lb(), 0.01);
        assertEquals(setPointRelativeToTsMax, relativeSetPointConstraint.ub(), 0.01);
        assertEquals(1, relativeSetPointConstraint.getCoefficient(setPointVariable1), 0.1);
        assertEquals(-1, relativeSetPointConstraint.getCoefficient(setPointVariable0), 0.1);

        checkPenaltyCost(pstRangeAction0, pstRangeAction1, setPointVariable0, setPointVariable1, state0, state1);
    }

    private void checkPenaltyCost(PstRangeAction pstRangeAction0, PstRangeAction pstRangeAction1, OpenRaoMPVariable setPointVariable0, OpenRaoMPVariable setPointVariable1, State state0, State state1) {
        // check penalty cost variable is updated correctly
        OpenRaoMPConstraint varConstraintPositive1 = linearProblem.getAbsoluteRangeActionVariationConstraint(
            pstRangeAction1,
            state1,
            LinearProblem.AbsExtension.POSITIVE
        );
        assertNotNull(varConstraintPositive1);
        assertEquals(0, varConstraintPositive1.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1, varConstraintPositive1.getCoefficient(setPointVariable0), 0.1);
        assertEquals(1, varConstraintPositive1.getCoefficient(setPointVariable1), 0.1);

        OpenRaoMPConstraint varConstraintNegative1 = linearProblem.getAbsoluteRangeActionVariationConstraint(
            pstRangeAction1,
            state1,
            LinearProblem.AbsExtension.NEGATIVE
        );
        assertNotNull(varConstraintPositive1);
        assertEquals(0, varConstraintNegative1.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, varConstraintNegative1.getCoefficient(setPointVariable0), 0.1);
        assertEquals(-1, varConstraintNegative1.getCoefficient(setPointVariable1), 0.1);

        // check penalty cost variable is unchanged for first time step
        OpenRaoMPConstraint varConstraintPositive0 = linearProblem.getAbsoluteRangeActionVariationConstraint(
            pstRangeAction0,
            state0,
            LinearProblem.AbsExtension.POSITIVE
        );
        assertNotNull(varConstraintPositive1);
        assertEquals(initialAlpha, varConstraintPositive0.lb(), DOUBLE_TOLERANCE);
        assertEquals(1, varConstraintPositive0.getCoefficient(setPointVariable0), 0.1);
        assertEquals(0, varConstraintPositive0.getCoefficient(setPointVariable1), 0.1);

        OpenRaoMPConstraint varConstraintNegative0 = linearProblem.getAbsoluteRangeActionVariationConstraint(
            pstRangeAction0,
            state0,
            LinearProblem.AbsExtension.NEGATIVE
        );
        assertNotNull(varConstraintPositive1);
        assertEquals(-initialAlpha, varConstraintNegative0.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1, varConstraintNegative0.getCoefficient(setPointVariable0), 0.1);
        assertEquals(0, varConstraintNegative0.getCoefficient(setPointVariable1), 0.1);
    }

    @Test
    void testFillPstDiscrete() {
        initializeDiscrete();
        State state0 = cnecs.get(0).getState();
        State state1 = cnecs.get(1).getState();
        PstRangeAction pstRangeAction0 = pstRangeActions.get(0);
        PstRangeAction pstRangeAction1 = pstRangeActions.get(1);

        OpenRaoMPVariable setPointVariable0 = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state0);
        OpenRaoMPVariable setPointVariable1 = linearProblem.getRangeActionSetpointVariable(pstRangeAction1, state1);

        OpenRaoMPVariable pstTapDownwardVariationVariable1 = linearProblem.getPstTapVariationVariable(pstRangeAction1, state1, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationVariable1 = linearProblem.getPstTapVariationVariable(pstRangeAction1, state1, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable pstTapDownwardVariationVariable0 = linearProblem.getPstTapVariationVariable(pstRangeAction0, state0, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable pstTapUpwardVariationVariable0 = linearProblem.getPstTapVariationVariable(pstRangeAction0, state0, LinearProblem.VariationDirectionExtension.UPWARD);

        // check constraint relative to timestamp
        OpenRaoMPConstraint relativeSetPointConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction1, state1, LinearProblem.RaRangeShrinking.FALSE);
        assertNotNull(relativeSetPointConstraint);
        assertEquals(tapRelativeToTsMin, relativeSetPointConstraint.lb(), 0.01);
        assertEquals(tapRelativeToTsMax, relativeSetPointConstraint.ub(), 0.01);
        assertEquals(1, relativeSetPointConstraint.getCoefficient(pstTapUpwardVariationVariable1), 0.1);
        assertEquals(-1, relativeSetPointConstraint.getCoefficient(pstTapDownwardVariationVariable1), 0.1);
        assertEquals(-1, relativeSetPointConstraint.getCoefficient(pstTapUpwardVariationVariable0), 0.1);
        assertEquals(1, relativeSetPointConstraint.getCoefficient(pstTapDownwardVariationVariable0), 0.1);

        // check penalty cost variable is updated correctly
        checkPenaltyCost(pstRangeAction0, pstRangeAction1, setPointVariable0, setPointVariable1, state0, state1);
    }

    @Test
    void testUpdateBetweenMip() {
        initializeDiscrete();
        updateMipAndChangeResult();
        State state1 = cnecs.get(1).getState();
        PstRangeAction pstRangeAction0 = pstRangeActions.get(0);
        PstRangeAction pstRangeAction1 = pstRangeActions.get(1);
        OpenRaoMPConstraint tapRelTimeStepConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction1, state1, LinearProblem.RaRangeShrinking.FALSE);

        assertEquals(tapRelativeToTsMin - TAP_BETWEEN_MIP + pstRangeAction0.convertAngleToTap(initialAlpha), tapRelTimeStepConstraint.lb(), 0.01);
        assertEquals(tapRelativeToTsMax - TAP_BETWEEN_MIP + pstRangeAction0.convertAngleToTap(initialAlpha), tapRelTimeStepConstraint.ub(), 0.01);
    }

    @Test
    void testImpactLaterTimeSteps() {
        initFourTimesteps();
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        buildLinearProblem();
        State state0 = cnecs.get(0).getState();
        State state1 = cnecs.get(1).getState();
        PstRangeAction pstRangeAction0 = pstRangeActions.get(0);
        PstRangeAction pstRangeAction1 = pstRangeActions.get(1);
        OpenRaoMPVariable setPointVariable0 = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state0);
        OpenRaoMPVariable setPointVariable1 = linearProblem.getRangeActionSetpointVariable(pstRangeAction1, state1);

        OpenRaoMPConstraint flowConstraint0 = linearProblem.getFlowConstraint(cnecs.get(0), Side.LEFT);
        assertEquals(-SENSI_CNEC, flowConstraint0.getCoefficient(setPointVariable0), 0.01);
        assertEquals(0, flowConstraint0.getCoefficient(setPointVariable1), 0.01);

        OpenRaoMPConstraint flowConstraint1 = linearProblem.getFlowConstraint(cnecs.get(1), Side.LEFT);
        assertEquals(-SENSI_CNEC, flowConstraint1.getCoefficient(setPointVariable0), 0.01);
        assertEquals(0, flowConstraint1.getCoefficient(setPointVariable1), 0.01);
        assertEquals(REF_FLOW_CNEC - SENSI_CNEC * initialAlpha, flowConstraint1.lb(), 0.01);
        assertEquals(REF_FLOW_CNEC - SENSI_CNEC * initialAlpha, flowConstraint1.ub(), 0.01);

        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnecs.get(2), Side.LEFT);
        assertEquals(-SENSI_CNEC, flowConstraint2.getCoefficient(setPointVariable0), 0.01);
        assertEquals(0, flowConstraint2.getCoefficient(setPointVariable1), 0.01);
        assertEquals(REF_FLOW_CNEC - SENSI_CNEC * initialAlpha, flowConstraint2.lb(), 0.01);
        assertEquals(REF_FLOW_CNEC - SENSI_CNEC * initialAlpha, flowConstraint2.ub(), 0.01);

        OpenRaoMPConstraint flowConstraint3 = linearProblem.getFlowConstraint(cnecs.get(3), Side.LEFT);
        assertEquals(0, flowConstraint3.getCoefficient(setPointVariable0), 0.01);
        assertEquals(-SENSI_CNEC, flowConstraint3.getCoefficient(setPointVariable1), 0.01);

    }
}
