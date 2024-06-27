///*
// * Copyright (c) 2020, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;
//
//import com.powsybl.iidm.network.Network;
//import com.powsybl.openrao.commons.OpenRaoException;
//import com.powsybl.openrao.commons.Unit;
//import com.powsybl.openrao.data.cracapi.Crac;
//import com.powsybl.openrao.data.cracapi.State;
//import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
//import com.powsybl.openrao.data.cracapi.cnec.Side;
//import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
//import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
//import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
//import com.powsybl.openrao.data.cracioapi.CracImporters;
//import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
//import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
//import com.powsybl.openrao.raoapi.parameters.RaoParameters;
//import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
//import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
//import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
//import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
//import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
//import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
///**
// *
// */
//// extends AbstractFillerTest
//class MultiTSFillerTest {
//    static final double DOUBLE_TOLERANCE = 1e-4;
//    static final double INFINITY_TOLERANCE = LinearProblem.infinity() * 0.001;
//
//    // data related to the two Cnecs
//    static final double MIN_FLOW_1 = -750.0;
//    static final double MAX_FLOW_1 = 750.0;
//
//    static final double REF_FLOW_CNEC1_IT1 = 500.0;
//    static final double REF_FLOW_CNEC2_IT1 = 300.0;
//    static final double REF_FLOW_CNEC1_IT2 = 400.0;
//    static final double REF_FLOW_CNEC2_IT2 = 350.0;
//
//    static final double SENSI_CNEC1_IT1 = 2.0;
//    static final double SENSI_CNEC2_IT1 = 5.0;
//    static final double SENSI_CNEC1_IT2 = 3.0;
//    static final double SENSI_CNEC2_IT2 = -7.0;
//
//    // data related to the Range Action
//    static final int TAP_INITIAL = 5;
//    static final int TAP_IT2 = -7;
//
//    static final String CNEC_0_ID = "BBE2AA1  FFR3AA1  1 - preventive - TS0"; // monitored on left side
//    static final String CNEC_1_ID = "BBE2AA1  FFR3AA1  1 - preventive - TS1"; // monitored on right side
//    static final String RANGE_ACTION_ID_0 = "pst_be - TS0";
//    static final String RANGE_ACTION_ID_1 = "pst_be - TS1";
//    static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";
//
//    FlowCnec cnec0;
//    FlowCnec cnec1;
//    PstRangeAction pstRangeAction0;
//    PstRangeAction pstRangeAction1;
//    FlowResult flowResult;
//    SensitivityResult sensitivityResult;
//    List<Crac> cracs = new ArrayList<>();
//    List<Network> networks = new ArrayList<>();
//    FlowCnec cnec1Ts1;
//    FlowCnec cnec2Ts1;
//    private LinearProblem linearProblem;
//    private CoreProblemFiller coreProblemFiller;
//    private MultiTSFiller multiTSFiller;
//    private RangeActionSetpointResult initialRangeActionSetpointResult;
//    // some additional data
//    private double setPointRelativeToTsMin;
//    private double setPointRelativeToTsMax;
//    private double initialAlpha;
//    RaoParameters raoParameters;
//
//    void init() {
//        // arrange some data for all fillers test
//        // crac and network
//        networks.add(Network.read("multi-ts/network/12NodesProdFR.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR.uct")));
//        networks.add(Network.read("multi-ts/network/12NodesProdNL.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdNL.uct")));
//        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-timesteps-test_0.json", getClass().getResourceAsStream("multi-ts/crac/crac-timesteps-test_0.json"), networks.get(0)));
//        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-timesteps-test_1.json", getClass().getResourceAsStream("multi-ts/crac/crac-timesteps-test_1.json"), networks.get(1)));
//
//
//        // get cnecs and rangeActions
//        cnec0 = cracs.get(0).getFlowCnec(CNEC_0_ID);
//        cnec1 = cracs.get(1).getFlowCnec(CNEC_1_ID);
//
//        pstRangeAction0 = cracs.get(0).getPstRangeAction(RANGE_ACTION_ID_0);
//        pstRangeAction1 = cracs.get(1).getPstRangeAction(RANGE_ACTION_ID_1);
//
//        flowResult = Mockito.mock(FlowResult.class);
//        when(flowResult.getFlow(cnec0, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT1);
//        when(flowResult.getFlow(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT1);
//
//        sensitivityResult = Mockito.mock(SensitivityResult.class);
//        when(sensitivityResult.getSensitivityValue(cnec0, Side.LEFT, pstRangeAction0, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT1);
//        when(sensitivityResult.getSensitivityValue(cnec1, Side.RIGHT, pstRangeAction0, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT1);
//        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
//        //???
//
//        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(pstSensitivityThreshold);
//        raoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
//        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(injectionSensitivityThreshold);
//        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
//
//    }
//
//    public void initSecondTimeStep() {
//        networkTs1 = NetworkImportsUtil.import12NodesNetwork();
//        cracTs1 = CracImporters.importCrac("crac/small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"), network);
//
//        // get cnec and rangeAction
//        cnec1Ts1 = cracTs1.getFlowCnec(CNEC_0_ID);
//        cnec2Ts1 = cracTs1.getFlowCnec(CNEC_1_ID);
//        pstRangeActionTs1 = cracTs1.getPstRangeAction(RANGE_ACTION_ID_0);
//
//        flowResult = Mockito.mock(FlowResult.class);
//        when(flowResult.getFlow(cnec0, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT1);
//        when(flowResult.getFlow(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT1);
//
//        sensitivityResult = Mockito.mock(SensitivityResult.class);
//        when(sensitivityResult.getSensitivityValue(cnec1Ts1, Side.LEFT, pstRangeActionTs1, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT1);
//        when(sensitivityResult.getSensitivityValue(cnec2Ts1, Side.RIGHT, pstRangeActionTs1, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT1);
//        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
//    }
//
//    @BeforeEach
//    public void setUp() {
//        init();
//        initSecondTimeStep();
//        // arrange some additional data
//        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
//        setPointRelativeToTsMin = cracs.get(1).getPstRangeAction(RANGE_ACTION_ID_1).getRanges().get(0).getMinTap();
//        setPointRelativeToTsMax = cracs.get(1).getPstRangeAction(RANGE_ACTION_ID_1).getRanges().get(0).getMaxTap();
//        initialAlpha = pstRangeAction0.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
//
//        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction0, initialAlpha));
//    }
//
//    private void buildLinearProblem() {
//        linearProblem = new LinearProblemBuilder()
//            .withProblemFiller(coreProblemFiller)
//            .withProblemFiller(multiTSFiller)
//            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
//            .build();
//        linearProblem.fill(flowResult, sensitivityResult);
//    }
//
//    private void initializeForPreventive(double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold) {
//        initializeCoreProblemFiller(Set.of(cnec0), pstSensitivityThreshold, hvdcSensitivityThreshold, injectionSensitivityThreshold, crac.getPreventiveState(), false);
//    }
//
//    private void initializeForGlobal() {
//        initializeCoreProblemFiller(Set.of(cnec0, cnec1), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), false);
//    }
//
//    private void initializeCoreProblemFiller(Set<FlowCnec> cnecs, double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold, State mainState, boolean raRangeShrinking) {
//        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
//        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(cnecs);
//        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainState);
//
//        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
//        cnecs.forEach(cnec -> rangeActions.put(cnec.getState(), Set.of(pstRangeAction0)));
//        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);
//
//
//        coreProblemFiller = new CoreProblemFiller(
//            optimizationPerimeter,
//            initialRangeActionSetpointResult,
//            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
//            rangeActionParameters,
//            Unit.MEGAWATT, raRangeShrinking);
//        buildLinearProblem();
//    }
//
//
//    private void initializeMultiTSFiller(Set<FlowCnec> cnecs, double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold, State mainState, boolean raRangeShrinking) {
//        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
//        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(cnecs);
//        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainState);
//
//        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
//        cnecs.forEach(cnec -> rangeActions.put(cnec.getState(), Set.of(pstRangeAction0)));
//        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);
//
//        RaoParameters raoParameters = new RaoParameters();
//        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(pstSensitivityThreshold);
//        raoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
//        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(injectionSensitivityThreshold);
//        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
//
//        multiTSFiller = new MultiTSFiller(
//            optimizationPerimeter,
//            List.of(network, networkTs1),
//            rangeActionParameters,
//            new RangeActionActivationResultImpl(initialRangeActionSetpointResult)
//        );
//        buildLinearProblem();
//    }
//
//    @Test
//    void fillTestContinuous() {
//        initializeForPreventive(1e-6, 1e-6, 1e-6);
//        State state0 = cnec0.getState();
//        State state1 = cnec1.getState();
//
//        OpenRaoMPVariable setPointVariable0 = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state0);
//        OpenRaoMPVariable setPointVariable1 = linearProblem.getRangeActionSetpointVariable(pstRangeAction1, state1);
//
//        // check constraint relative to timestamp
//        OpenRaoMPConstraint relativeSetPointConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction1, state1, LinearProblem.RaRangeShrinking.FALSE);
//        assertNotNull(relativeSetPointConstraint);
//        assertEquals(setPointRelativeToTsMin, relativeSetPointConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(setPointRelativeToTsMax, relativeSetPointConstraint.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, relativeSetPointConstraint.getCoefficient(setPointVariable1), 0.1);
//        assertEquals(-1, relativeSetPointConstraint.getCoefficient(setPointVariable0), 0.1);
//
//        // --------
//
//
//
//        // check range action absolute variation variable
//        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction0, state1);
//        assertNotNull(absoluteVariationVariable);
//        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
//        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow variable for cnec1
//        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec0, Side.LEFT);
//        assertNotNull(flowVariable);
//        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec1
//        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        assertNotNull(flowConstraint);
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
//        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(relativeSetPointConstraint), DOUBLE_TOLERANCE);
//
//        // check flow variable for cnec2 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.RIGHT));
//        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec2 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.RIGHT));
//        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());
//
//        // check absolute variation constraints
//        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state1, LinearProblem.AbsExtension.NEGATIVE);
//        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state1, LinearProblem.AbsExtension.POSITIVE);
//        assertNotNull(absoluteVariationConstraint1);
//        assertNotNull(absoluteVariationConstraint2);
//        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), INFINITY_TOLERANCE);
//        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), INFINITY_TOLERANCE);
//
//        // check the number of variables and constraints
//        // total number of variables 4 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point and variation)
//        // total number of constraints 4 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 2 per range action (absolute variation constraints)
//        assertEquals(3, linearProblem.numVariables());
//        assertEquals(3, linearProblem.numConstraints());
//    }
//
//    @Test
//    void fillTestOnPreventiveFiltered() {
//        initializeForPreventive(2.5, 2.5, 2.5);
//        State state = cnec0.getState();
//
//        // check range action setpoint variable
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state);
//        assertNotNull(setPointVariable);
//        assertEquals(setPointRelativeToTsMin, setPointVariable.lb(), DOUBLE_TOLERANCE);
//        assertEquals(setPointRelativeToTsMax, setPointVariable.ub(), DOUBLE_TOLERANCE);
//
//        // check range action absolute variation variable
//        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction0, state);
//        assertNotNull(absoluteVariationVariable);
//        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
//        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow variable for cnec1
//        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec0, Side.LEFT);
//        assertNotNull(flowVariable);
//        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec1
//        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        assertNotNull(flowConstraint);
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
//        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
//        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
//
//        // check flow variable for cnec2 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.RIGHT));
//        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec2 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.RIGHT));
//        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());
//
//        // check absolute variation constraints
//        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state, LinearProblem.AbsExtension.NEGATIVE);
//        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state, LinearProblem.AbsExtension.POSITIVE);
//        assertNotNull(absoluteVariationConstraint1);
//        assertNotNull(absoluteVariationConstraint2);
//        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), INFINITY_TOLERANCE);
//        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), INFINITY_TOLERANCE);
//
//        // check the number of variables and constraints
//        // total number of variables 4 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point and variation)
//        // total number of constraints 4 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 2 per range action (absolute variation constraints)
//        assertEquals(3, linearProblem.numVariables());
//        assertEquals(3, linearProblem.numConstraints());
//    }
//
//    @Test
//    void fillTestOnCurative() {
//        initializeCoreProblemFiller(Set.of(cnec1), 1e-6, 1e-6, 1e-6, cnec1.getState(), false);
//        State state = cnec1.getState();
//
//        // check range action setpoint variable
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state);
//        assertNotNull(setPointVariable);
//        assertEquals(setPointRelativeToTsMin, setPointVariable.lb(), DOUBLE_TOLERANCE);
//        assertEquals(setPointRelativeToTsMax, setPointVariable.ub(), DOUBLE_TOLERANCE);
//
//        // check range action absolute variation variable
//        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction0, state);
//        assertNotNull(absoluteVariationVariable);
//        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
//        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow variable for cnec1 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec0, Side.LEFT));
//        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec1 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec0, Side.LEFT));
//        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());
//
//        // check flow variable for cnec2
//        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec1, Side.RIGHT);
//        assertNotNull(flowVariable2);
//        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec2
//        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec1, Side.RIGHT);
//        assertNotNull(flowConstraint2);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//
//        // check absolute variation constraints
//        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state, LinearProblem.AbsExtension.NEGATIVE);
//        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, state, LinearProblem.AbsExtension.POSITIVE);
//        assertNotNull(absoluteVariationConstraint1);
//        assertNotNull(absoluteVariationConstraint2);
//        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), INFINITY_TOLERANCE);
//        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), INFINITY_TOLERANCE);
//
//        // check the number of variables and constraints
//        // total number of variables 4 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point and variation)
//        // total number of constraints 4 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 2 per range action (absolute variation constraints)
//        assertEquals(3, linearProblem.numVariables());
//        assertEquals(3, linearProblem.numConstraints());
//    }
//
//    @Test
//    void fillTestOnGlobal() {
//        initializeForGlobal();
//        State prevState = cnec0.getState();
//        State curState = cnec1.getState();
//
//        // check range action setpoint variable for preventive state
//        OpenRaoMPVariable prevSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, prevState);
//        assertNotNull(prevSetPointVariable);
//        assertEquals(setPointRelativeToTsMin, prevSetPointVariable.lb(), DOUBLE_TOLERANCE);
//        assertEquals(setPointRelativeToTsMax, prevSetPointVariable.ub(), DOUBLE_TOLERANCE);
//
//        // check range action setpoint variable for curative state
//        OpenRaoMPVariable curSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, curState);
//        assertNotNull(curSetPointVariable);
//        assertEquals(setPointRelativeToTsMin, curSetPointVariable.lb(), DOUBLE_TOLERANCE);
//        assertEquals(setPointRelativeToTsMax, curSetPointVariable.ub(), DOUBLE_TOLERANCE);
//
//        // check range action absolute variation variable for preventive state
//        OpenRaoMPVariable prevAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction0, prevState);
//        assertNotNull(prevAbsoluteVariationVariable);
//        assertEquals(0, prevAbsoluteVariationVariable.lb(), 0.01);
//        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationVariable.ub(), INFINITY_TOLERANCE);
//
//        // check range action absolute variation variable for curative state
//        OpenRaoMPVariable curAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction0, curState);
//        assertNotNull(curAbsoluteVariationVariable);
//        assertEquals(0, curAbsoluteVariationVariable.lb(), 0.01);
//        assertEquals(LinearProblem.infinity(), curAbsoluteVariationVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow variable for cnec1
//        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec0, Side.LEFT);
//        assertNotNull(flowVariable);
//        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec1
//        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        assertNotNull(flowConstraint);
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
//        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);
//
//        // check flow variable for cnec2
//        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec1, Side.RIGHT);
//        assertNotNull(flowVariable2);
//        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec2
//        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec1, Side.RIGHT);
//        assertNotNull(flowConstraint2);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);
//
//        // check absolute variation constraints for preventive state
//        OpenRaoMPConstraint prevAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, prevState, LinearProblem.AbsExtension.NEGATIVE);
//        OpenRaoMPConstraint prevAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, prevState, LinearProblem.AbsExtension.POSITIVE);
//        assertNotNull(prevAbsoluteVariationConstraint1);
//        assertNotNull(prevAbsoluteVariationConstraint2);
//        assertEquals(-initialAlpha, prevAbsoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationConstraint1.ub(), INFINITY_TOLERANCE);
//        assertEquals(initialAlpha, prevAbsoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationConstraint2.ub(), INFINITY_TOLERANCE);
//
//        // check absolute variation constraints for curative state
//        OpenRaoMPConstraint curAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, curState, LinearProblem.AbsExtension.NEGATIVE);
//        OpenRaoMPConstraint curAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction0, curState, LinearProblem.AbsExtension.POSITIVE);
//        assertNotNull(curAbsoluteVariationConstraint1);
//        assertNotNull(curAbsoluteVariationConstraint2);
//        assertEquals(0, curAbsoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(1., curAbsoluteVariationConstraint1.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);
//        assertEquals(-1., curAbsoluteVariationConstraint1.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);
//        assertEquals(1., curAbsoluteVariationConstraint1.getCoefficient(curAbsoluteVariationVariable), DOUBLE_TOLERANCE);
//        assertEquals(0, curAbsoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(-1., curAbsoluteVariationConstraint2.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);
//        assertEquals(1., curAbsoluteVariationConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);
//        assertEquals(1., curAbsoluteVariationConstraint2.getCoefficient(curAbsoluteVariationVariable), DOUBLE_TOLERANCE);
//
//        // check the number of variables and constraints
//        // total number of variables 6 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point and variation)
//        // total number of constraints 7 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 2 per range action (absolute variation constraints)
//        //      - 1 for curative range action (relative variation constraint)
//        assertEquals(6, linearProblem.numVariables());
//        assertEquals(7, linearProblem.numConstraints());
//    }
//
//    private void updateLinearProblem() {
//        // arrange some additional data
//        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);
//        initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
//
//        when(flowResult.getFlow(cnec0, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT2);
//        when(flowResult.getFlow(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT2);
//        when(sensitivityResult.getSensitivityValue(cnec0, Side.LEFT, pstRangeAction0, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT2);
//        when(sensitivityResult.getSensitivityValue(cnec1, Side.RIGHT, pstRangeAction0, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT2);
//
//        // update the problem
//        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction0, initialAlpha));
//        linearProblem.updateBetweenSensiIteration(flowResult, sensitivityResult, new RangeActionActivationResultImpl(rangeActionSetpointResult));
//    }
//
//    @Test
//    void updateTestOnPreventive() {
//        initializeForPreventive(1e-6, 1e-6, 1e-6);
//        State state = cnec0.getState();
//        // update the problem with new data
//        updateLinearProblem();
//
//        // some additional data
//        final double currentAlpha = pstRangeAction0.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
//
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state);
//
//        // check flow variable for cnec1
//        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec0, Side.LEFT);
//        assertNotNull(flowVariable);
//        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec1
//        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        assertNotNull(flowConstraint);
//        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//
//        // check flow variable for cnec2 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.RIGHT));
//        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec2 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.RIGHT));
//        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());
//
//        // check the number of variables and constraints
//        // No iterative relative variation constraint should be created since CoreProblemFiller.raRangeShrinking = false
//        // total number of variables 3 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point)
//        // total number of constraints 3 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 2 per range action (absolute variation constraints)
//
//        assertEquals(3, linearProblem.numVariables());
//        assertEquals(3, linearProblem.numConstraints());
//    }
//
//    @Test
//    void updateTestOnCurativeWithRaRangeShrinking() {
//        initializeCoreProblemFiller(Set.of(cnec1), 1e-6, 1e-6, 1e-6, cnec1.getState(), true);
//        State state = cnec1.getState();
//        // update the problem with new data
//        updateLinearProblem();
//
//        // some additional data
//        final double currentAlpha = pstRangeAction0.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
//
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, state);
//
//        // check flow variable for cnec1 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec0, Side.LEFT));
//        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec1 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec0, Side.LEFT));
//        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());
//
//        // check flow variable for cnec2
//        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec1, Side.RIGHT);
//        assertNotNull(flowVariable2);
//        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec2
//        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec1, Side.RIGHT);
//        assertNotNull(flowConstraint2);
//        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//
//        // check the number of variables and constraints
//        // total number of variables 3 :
//        //      - 1 per CNEC (flow)
//        //      - 2 per range action (set-point and variation)
//        // total number of constraints 4 :
//        //      - 1 per CNEC (flow constraint)
//        //      - 3 per range action (absolute variation constraints and iterative relative variation constraint: created before 2nd iteration)
//        assertEquals(3, linearProblem.numVariables());
//        assertEquals(4, linearProblem.numConstraints());
//
//        // assert that no other constraint is created after 2nd iteration
//        updateLinearProblem();
//        assertEquals(4, linearProblem.numConstraints());
//    }
//
//    @Test
//    void testSensitivityFilter1() {
//        OpenRaoMPConstraint flowConstraint;
//        OpenRaoMPVariable rangeActionSetpoint;
//        when(flowResult.getPtdfZonalSum(cnec0, Side.LEFT)).thenReturn(0.5);
//
//        // (sensi = 2) < 2.5 should be filtered
//        when(flowResult.getMargin(cnec0, Unit.MEGAWATT)).thenReturn(-1.0);
//        initializeCoreProblemFiller(Set.of(cnec0), 2.5, 2.5, 2.5, crac.getPreventiveState(), false);
//        flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, cnec0.getState());
//        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
//        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);
//    }
//
//    @Test
//    void testSensitivityFilter2() {
//        OpenRaoMPConstraint flowConstraint;
//        OpenRaoMPVariable rangeActionSetpoint;
//        when(flowResult.getPtdfZonalSum(cnec0, Side.LEFT)).thenReturn(0.5);
//        Map<Integer, Double> tapToAngle = pstRangeAction0.getTapToAngleConversionMap();
//
//        // (sensi = 2) > 1/.5 should not be filtered
//        when(flowResult.getMargin(cnec0, Side.LEFT, Unit.MEGAWATT)).thenReturn(-1.0);
//        initializeCoreProblemFiller(Set.of(cnec0), 1.5, 1.5, 1.5, crac.getPreventiveState(), false);
//        flowConstraint = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, cnec0.getState());
//        assertEquals(-2, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
//        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.lb(), DOUBLE_TOLERANCE);
//        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.ub(), DOUBLE_TOLERANCE);
//    }
//
//    @Test
//    void testFilterCnecWithSensiFailure() {
//        // cnec1 has a failed state, cnec2 has a succeeded state
//        // only cnec2's flow variables & constraints must be added to MIP
//        when(sensitivityResult.getSensitivityStatus(cnec0.getState())).thenReturn(ComputationStatus.FAILURE);
//        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.DEFAULT);
//        initializeCoreProblemFiller(Set.of(cnec0, cnec1), 1e-6, 1e-6, 1e-6, cnec0.getState(), false);
//
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, cnec1.getState());
//
//        // check flow variable for cnec1 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec0, Side.LEFT));
//        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec1 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec0, Side.LEFT));
//        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());
//
//        // check flow variable for cnec2
//        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec1, Side.RIGHT);
//        assertNotNull(flowVariable2);
//        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec2
//        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec1, Side.RIGHT);
//        assertNotNull(flowConstraint2);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//    }
//
//    @Test
//    void testFilterCnecWithSensiFailureAndUpdateWithoutChange() {
//        // cnec1 has a failed state, cnec2 has a succeeded state
//        // only cnec2's flow variables & constraints must be added to MIP
//        when(sensitivityResult.getSensitivityStatus(cnec0.getState())).thenReturn(ComputationStatus.FAILURE);
//        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.DEFAULT);
//        initializeCoreProblemFiller(Set.of(cnec0, cnec1), 1e-6, 1e-6, 1e-6, cnec0.getState(), false);
//
//        updateLinearProblem();
//
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, cnec1.getState());
//
//        // check flow variable for cnec1 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec0, Side.LEFT));
//        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec1 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec0, Side.LEFT));
//        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());
//
//        // check flow variable for cnec2
//        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec1, Side.RIGHT);
//        assertNotNull(flowVariable2);
//        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), INFINITY_TOLERANCE);
//
//        // check flow constraint for cnec2
//        final double currentAlpha = pstRangeAction0.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
//        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec1, Side.RIGHT);
//        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//    }
//
//    @Test
//    void testFilterCnecWithSensiFailureAndUpdateWithChange() {
//        // cnec1 has a failed state, cnec2 has a succeeded state
//        // only cnec2's flow variables & constraints must be added to MIP
//        when(sensitivityResult.getSensitivityStatus(cnec0.getState())).thenReturn(ComputationStatus.FAILURE);
//        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.DEFAULT);
//        initializeCoreProblemFiller(Set.of(cnec0, cnec1), 1e-6, 1e-6, 1e-6, cnec0.getState(), false);
//
//        // invert sensitivity failure statuses & update
//        // only cnec1's flow variables & constraints must be added to MIP
//        when(sensitivityResult.getSensitivityStatus(cnec0.getState())).thenReturn(ComputationStatus.DEFAULT);
//        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.FAILURE);
//        updateLinearProblem();
//
//        // check flow variable for cnec2 does not exist
//        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.RIGHT));
//        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());
//
//        // check flow constraint for cnec2 does not exist
//        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.RIGHT));
//        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());
//
//        // check flow variable for cnec1
//        OpenRaoMPVariable flowVariable1 = linearProblem.getFlowVariable(cnec0, Side.LEFT);
//        assertNotNull(flowVariable1);
//        assertEquals(-LinearProblem.infinity(), flowVariable1.lb(), INFINITY_TOLERANCE);
//        assertEquals(LinearProblem.infinity(), flowVariable1.ub(), INFINITY_TOLERANCE);
//
//        final double currentAlpha = pstRangeAction0.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
//        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction0, cnec0.getState());
//
//        // check flow constraint for cnec1
//        OpenRaoMPConstraint flowConstraint1 = linearProblem.getFlowConstraint(cnec0, Side.LEFT);
//        assertNotNull(flowConstraint1);
//        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.lb(), DOUBLE_TOLERANCE);
//        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.ub(), DOUBLE_TOLERANCE);
//        assertEquals(1, flowConstraint1.getCoefficient(flowVariable1), DOUBLE_TOLERANCE);
//        assertEquals(-SENSI_CNEC1_IT2, flowConstraint1.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
//    }
//}
