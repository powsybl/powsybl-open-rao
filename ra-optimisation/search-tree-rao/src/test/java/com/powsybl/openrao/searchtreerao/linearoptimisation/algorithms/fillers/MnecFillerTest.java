/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.Solver;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.*;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class MnecFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private FlowCnec mnec1;
    private FlowCnec mnec2;
    private FlowCnec mnec3;

    @BeforeEach
    public void setUp() throws IOException {
        init();
        mnec1 = crac.newFlowCnec()
                .withId("MNEC1 - N - preventive")
                .withNetworkElement("DDE2AA1  NNL3AA1  1")
                .newThreshold()
                .withMin(-1000.)
                .withSide(TwoSides.TWO)
                .withMax(1000.0)
                .withUnit(Unit.MEGAWATT)
                .add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withInstant(PREVENTIVE_INSTANT_ID)
                .add();

        mnec2 = crac.newFlowCnec()
                .withId("MNEC2 - N - preventive")
                .withNetworkElement("NNL2AA1  BBE3AA1  1")
                .newThreshold()
                .withMin(-100.)
                .withSide(TwoSides.ONE)
                .withMax(100.0)
                .withUnit(Unit.MEGAWATT)
                .add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withInstant(PREVENTIVE_INSTANT_ID)
                .add();

        mnec3 = crac.newFlowCnec()
                .withId("MNEC3 - curative")
                .withNetworkElement("NNL2AA1  BBE3AA1  1")
                .newThreshold().withMin(-100.).withSide(TwoSides.ONE).withMax(100.0).withUnit(Unit.MEGAWATT).add()
                .newThreshold().withMin(-100.).withSide(TwoSides.TWO).withMax(100.0).withUnit(Unit.MEGAWATT).add()
                .withNominalVoltage(380.)
                .withOptimized(true)
                .withMonitored(true)
                .withContingency("N-1 NL1-NL3")
                .withInstant(crac.getInstant(InstantKind.CURATIVE).getId())
                .add();

        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Collections.emptyMap());

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(mnec1, mnec2, mnec3));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Collections.emptySet());
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(0.01);

        coreProblemFiller = new CoreProblemFiller(
                optimizationPerimeter,
                initialRangeActionSetpointResult,
                raoParameters.getRangeActionsOptimizationParameters(),
                null,
                Unit.MEGAWATT,
            false, PstModel.CONTINUOUS);
    }

    private void fillProblemWithFiller(Unit unit) {
        MnecParametersExtension parameters = new MnecParametersExtension();
        parameters.setAcceptableMarginDecrease(50);
        parameters.setViolationCost(10);
        parameters.setConstraintAdjustmentCoefficient(3.5);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(mnec1, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(900.);
        when(flowResult.getFlow(mnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(-200.);
        MnecFiller mnecFiller = new MnecFiller(
                flowResult,
                Set.of(mnec1, mnec2, mnec3),
                unit,
                parameters);
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(mnecFiller)
                .withSolver(Solver.SCIP)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void testAddMnecViolationVariables() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getFlowCnecs().forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            if (cnec.isMonitored()) {
                OpenRaoMPVariable variable = linearProblem.getMnecViolationVariable(cnec, side);
                assertNotNull(variable);
                assertEquals(0, variable.lb(), DOUBLE_TOLERANCE);
                assertEquals(linearProblem.infinity(), variable.ub(), linearProblem.infinity() * 1e-3);
            } else {
                Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecViolationVariable(cnec, side));
                assertEquals(String.format("Variable %s has not been created yet", LinearProblemIdGenerator.mnecViolationVariableId(cnec, side)), e.getMessage());
            }
        }));
    }

    @Test
    void testAddMnecMinFlowConstraints() {
        fillProblemWithFiller(Unit.MEGAWATT);

        crac.getFlowCnecs().stream().filter(cnec -> !cnec.isMonitored()).forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecFlowConstraint(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD));
            assertEquals(String.format("Constraint %s has not been created yet", LinearProblemIdGenerator.mnecFlowConstraintId(cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD)), e.getMessage());
        }));

        OpenRaoMPConstraint ct1Max = linearProblem.getMnecFlowConstraint(mnec1, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct1Max);
        assertEquals(-linearProblem.infinity(), ct1Max.lb(), linearProblem.infinity() * 1e-3);
        double mnec1MaxFlow = 1000 - 3.5;
        assertEquals(mnec1MaxFlow, ct1Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Max.getCoefficient(linearProblem.getFlowVariable(mnec1, TwoSides.TWO)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct1Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec1, TwoSides.TWO)), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint ct1Min = linearProblem.getMnecFlowConstraint(mnec1, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct1Min);
        double mnec1MinFlow = -1000 + 3.5;
        assertEquals(mnec1MinFlow, ct1Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), ct1Min.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getFlowVariable(mnec1, TwoSides.TWO)), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec1, TwoSides.TWO)), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint ct2Max = linearProblem.getMnecFlowConstraint(mnec2, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct2Max);
        assertEquals(-linearProblem.infinity(), ct2Max.lb(), linearProblem.infinity() * 1e-3);
        double mnec2MaxFlow = 100 - 3.5;
        assertEquals(mnec2MaxFlow, ct2Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Max.getCoefficient(linearProblem.getFlowVariable(mnec2, TwoSides.ONE)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct2Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec2, TwoSides.ONE)), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint ct2Min = linearProblem.getMnecFlowConstraint(mnec2, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct2Min);
        double mnec2MinFlow = -250 + 3.5;
        assertEquals(mnec2MinFlow, ct2Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), ct2Min.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getFlowVariable(mnec2, TwoSides.ONE)), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec2, TwoSides.ONE)), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddMnecPenaltyCostMW() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getFlowCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec, side);
            assertEquals(10.0 / cnec.getMonitoredSides().size(), linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        }));
    }

    @Test
    void testAddMnecPenaltyCostA() {
        fillProblemWithFiller(Unit.AMPERE);
        crac.getFlowCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec, side);
            assertEquals(10.0 / 0.658179 / cnec.getMonitoredSides().size(), linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        }));
    }

    @Test
    void testFilterCnecWithNoInitialFlow() {
        MnecParametersExtension parameters = new MnecParametersExtension();
        parameters.setAcceptableMarginDecrease(50);
        parameters.setViolationCost(10);
        parameters.setConstraintAdjustmentCoefficient(3.5);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(mnec1, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(900.);
        when(flowResult.getFlow(mnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-200.);
        when(flowResult.getFlow(mnec3, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Double.NaN);
        when(sensitivityResult.getSensitivityStatus(crac.getState("N-1 NL1-NL3", crac.getInstant(InstantKind.CURATIVE)))).thenReturn(ComputationStatus.FAILURE);
        MnecFiller mnecFiller = new MnecFiller(
            flowResult,
            Set.of(mnec1, mnec2, mnec3),
            Unit.MEGAWATT,
            parameters);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(mnecFiller)
            .withSolver(Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecFlowConstraint(mnec3, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint MNEC3 - curative_one_mnecflow_above_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecFlowConstraint(mnec3, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint MNEC3 - curative_one_mnecflow_below_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecFlowConstraint(mnec3, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint MNEC3 - curative_two_mnecflow_above_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecFlowConstraint(mnec3, TwoSides.TWO, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint MNEC3 - curative_two_mnecflow_below_threshold_constraint has not been created yet", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecViolationVariable(mnec3, TwoSides.ONE));
        assertEquals("Variable MNEC3 - curative_one_mnecviolation_variable has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMnecViolationVariable(mnec3, TwoSides.TWO));
        assertEquals("Variable MNEC3 - curative_two_mnecviolation_variable has not been created yet", e.getMessage());
    }
}
