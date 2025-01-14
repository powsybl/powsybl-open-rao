/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com}
 */
class PowerGradientConstraintFillerTest {
    private LinearProblemBuilder linearProblemBuilder = new LinearProblemBuilder().withSolver(RangeActionsOptimizationParameters.Solver.SCIP);
    private LinearProblem linearProblem;
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 1, 9, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 1, 9, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 1, 9, 18, 21, 0, 0, ZoneOffset.UTC);
    InterTemporalRaoInput input;
    RaoParameters parameters;

    public void createThreeTSInput() throws IOException {
        Network network1 = Network.read("4Nodes.uct", PowerGradientConstraintFillerTest.class.getResourceAsStream("/network/4Nodes.uct"));
        Network network2 = Network.read("4Nodes.uct", PowerGradientConstraintFillerTest.class.getResourceAsStream("/network/4Nodes.uct"));
        Network network3 = Network.read("4Nodes.uct", PowerGradientConstraintFillerTest.class.getResourceAsStream("/network/4Nodes.uct"));

        crac1 = Crac.read("crac-1600.json", PowerGradientConstraintFillerTest.class.getResourceAsStream("/crac/crac-1600.json"), network1);
        crac2 = Crac.read("crac-1700.json", PowerGradientConstraintFillerTest.class.getResourceAsStream("/crac/crac-1700.json"), network2);
        crac3 = Crac.read("crac-1800.json", PowerGradientConstraintFillerTest.class.getResourceAsStream("/crac/crac-1800.json"), network3);

        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        RaoInput raoInput2 = RaoInput.build(network2, crac2).build();
        RaoInput raoInput3 = RaoInput.build(network3, crac3).build();

        //create powerGradientConstraint
        PowerGradient powerGradientFFR1AA1 = new PowerGradient("FFR1AA1 _load", -500.0, 500.0);
        PowerGradient powerGradientFFR2AA1 = new PowerGradient("FFR2AA1 _generator", -100.0, 200.0);
        PowerGradient powerGradientFFR3AA1 = new PowerGradient("FFR3AA1 _load", -500.0, 500.0);

        input = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3)), Set.of(powerGradientFFR1AA1, powerGradientFFR3AA1, powerGradientFFR2AA1));

        parameters = new RaoParameters();
    }

    private void createOneTSInput() throws IOException {
        Network network1 = Network.read("4Nodes.uct", PowerGradientConstraintFillerTest.class.getResourceAsStream("/network/4Nodes.uct"));
        crac1 = Crac.read("crac-1600.json", PowerGradientConstraintFillerTest.class.getResourceAsStream("/crac/crac-1600.json"), network1);
        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();

        //create powerGradientConstraint
        PowerGradient powerGradientFFR1AA1 = new PowerGradient("FFR1AA1 _load", -500.0, 500.0);
        PowerGradient powerGradientFFR2AA1 = new PowerGradient("FFR2AA1 _generator", -100.0, 200.0);
        PowerGradient powerGradientFFR3AA1 = new PowerGradient("FFR3AA1 _load", -500.0, 500.0);

        input = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(timestamp1, raoInput1)), Set.of(powerGradientFFR1AA1, powerGradientFFR2AA1, powerGradientFFR3AA1));
        parameters = new RaoParameters();
    }

    private void createCoreProblemFillers() {
        input.getRaoInputs().getDataPerTimestamp().entrySet().forEach(entry -> {
            Crac crac = entry.getValue().getCrac();
            OptimizationPerimeter optimizationPerimeter = new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                crac.getFlowCnecs(),
                Set.of(),
                crac.getNetworkActions(crac.getPreventiveState()),
                crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE)
            );

            RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(parameters);
            Map<RangeAction<?>, Double> map = new HashMap<>();
            crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE).forEach(action -> map.put(action, 0.0));
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(map);
            CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
                optimizationPerimeter,
                rangeActionSetpointResult,
                rangeActionParameters,
                Unit.MEGAWATT,
                false,
                RangeActionsOptimizationParameters.PstModel.CONTINUOUS
            );
            linearProblemBuilder.withProblemFiller(coreProblemFiller);
        });
    }

    private void createPowerGradientConstraintFiller() {
        PowerGradientConstraintFiller powerGradientConstraintFiller = new PowerGradientConstraintFiller(input);
        linearProblemBuilder.withProblemFiller(powerGradientConstraintFiller);
    }

    private void buildAndFillLinearProblem() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(any(), any(), any())).thenReturn(10.0);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        linearProblem = linearProblemBuilder.build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void testPowerGradientFiller() throws IOException {
        createOneTSInput();
        createCoreProblemFillers();
        createPowerGradientConstraintFiller();
        buildAndFillLinearProblem();

        // check generator power variable
        assertNotNull(linearProblem.getGeneratorPowerVariable("FFR1AA1 _load", timestamp1));
        assertNotNull(linearProblem.getGeneratorPowerVariable("FFR2AA1 _generator", timestamp1));
        assertNotNull(linearProblem.getGeneratorPowerVariable("FFR3AA1 _load", timestamp1));

        OpenRaoMPConstraint fr1Timestamp1PowerConstraint = linearProblem.getGeneratorPowerConstraint("FFR1AA1 _load", timestamp1);
        OpenRaoMPConstraint fr2Timestamp1PowerConstraint = linearProblem.getGeneratorPowerConstraint("FFR2AA1 _generator", timestamp1);
        OpenRaoMPConstraint fr3Timestamp1PowerConstraint = linearProblem.getGeneratorPowerConstraint("FFR3AA1 _load", timestamp1);

        // TODO: check the number of generator power constraint == 3 ? How

        assertNotNull(fr1Timestamp1PowerConstraint);
        assertNotNull(fr2Timestamp1PowerConstraint);
        assertNotNull(fr3Timestamp1PowerConstraint); //constraint created even if no injection action defined on this element
        assertThrows(OpenRaoException.class, () -> linearProblem.getGeneratorPowerVariable("FFR4AA1 _load", timestamp1)); //No power gradient constraint but injection range action defined on it

        //check bound
        assertEquals(500.0, fr1Timestamp1PowerConstraint.ub());
        assertEquals(500.0, fr1Timestamp1PowerConstraint.lb());
        assertEquals(1000.0, fr2Timestamp1PowerConstraint.ub());
        assertEquals(1000.0, fr2Timestamp1PowerConstraint.lb());
        assertEquals(500.0, fr3Timestamp1PowerConstraint.ub());
        assertEquals(500.0, fr3Timestamp1PowerConstraint.lb());

        //check Coefficient for injection action variable
        assertEquals(0, fr1Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.UPWARD)), 1e-5);
        assertEquals(0, fr1Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.DOWNWARD)), 1e-5);
        assertEquals(1.0, fr2Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.UPWARD)), 1e-5);
        assertEquals(-1.0, fr2Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.DOWNWARD)), 1e-5);
        assertEquals(-0.4, fr3Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.UPWARD)), 1e-5);
        assertEquals(0.4, fr3Timestamp1PowerConstraint.getCoefficient(linearProblem.getRangeActionVariationVariable(crac1.getInjectionRangeAction("redispatchingAction1600"), crac1.getPreventiveState(), LinearProblem.VariationDirectionExtension.DOWNWARD)), 1e-5);
    }

    @Test
    void testPowerGradientConstraintFiller() throws IOException {
        createThreeTSInput();
        createCoreProblemFillers();
        createPowerGradientConstraintFiller();
        buildAndFillLinearProblem();

        // check the power gradient constraint, Expect two gradient constraints per generator
        OpenRaoMPConstraint powerGradientConstraintFR1TS12 = linearProblem.getGeneratorPowerGradientConstraint("FFR1AA1 _load", timestamp2, timestamp1);
        assertNotNull(powerGradientConstraintFR1TS12);
        assertEquals(-500.0, powerGradientConstraintFR1TS12.lb());
        assertEquals(500.0, powerGradientConstraintFR1TS12.ub());
        OpenRaoMPConstraint powerGradientConstraintFR1TS23 = linearProblem.getGeneratorPowerGradientConstraint("FFR1AA1 _load", timestamp3, timestamp2);
        assertNotNull(powerGradientConstraintFR1TS23);
        assertEquals(-500.0, powerGradientConstraintFR1TS23.lb());
        assertEquals(500.0, powerGradientConstraintFR1TS23.ub());
        OpenRaoMPConstraint powerGradientConstraintFR2TS12 = linearProblem.getGeneratorPowerGradientConstraint("FFR2AA1 _generator", timestamp2, timestamp1);
        assertNotNull(powerGradientConstraintFR2TS12);
        assertEquals(-100.0, powerGradientConstraintFR2TS12.lb());
        assertEquals(200.0, powerGradientConstraintFR2TS12.ub());
        OpenRaoMPConstraint powerGradientConstraintFR2TS23 = linearProblem.getGeneratorPowerGradientConstraint("FFR2AA1 _generator", timestamp3, timestamp2);
        assertNotNull(powerGradientConstraintFR2TS23);
        assertEquals(-100.0, powerGradientConstraintFR2TS23.lb());
        assertEquals(200.0, powerGradientConstraintFR2TS23.ub());
        OpenRaoMPConstraint powerGradientConstraintFR3TS12 = linearProblem.getGeneratorPowerGradientConstraint("FFR3AA1 _load", timestamp2, timestamp1);
        assertNotNull(powerGradientConstraintFR3TS12);
        assertEquals(-500.0, powerGradientConstraintFR3TS12.lb());
        assertEquals(500.0, powerGradientConstraintFR3TS12.ub());
        OpenRaoMPConstraint powerGradientConstraintFR3TS23 = linearProblem.getGeneratorPowerGradientConstraint("FFR3AA1 _load", timestamp3, timestamp2);
        assertNotNull(powerGradientConstraintFR3TS23);
        assertEquals(-500.0, powerGradientConstraintFR3TS23.lb());
        assertEquals(500.0, powerGradientConstraintFR3TS23.ub());

        // wrong previous timesteps
        assertThrows(OpenRaoException.class, () -> linearProblem.getGeneratorPowerGradientConstraint("FFR1AA1 _load", timestamp3, timestamp1));
    }

    @Test
    void testNoPowerGradientConstraintButInjectionAction() throws IOException {
        Network network1 = Network.read("4Nodes.uct", PowerGradientConstraintFillerTest.class.getResourceAsStream("/network/4Nodes.uct"));
        crac1 = Crac.read("crac-1600.json", PowerGradientConstraintFillerTest.class.getResourceAsStream("/crac/crac-1600.json"), network1);
        RaoInput raoInput1 = RaoInput.build(network1, crac1).build();
        //create powerGradientConstraint
        PowerGradient powerGradientFFR1AA1 = new PowerGradient("FFR1AA1 _load", -500.0, 500.0);
        PowerGradient powerGradientFFR2AA1 = new PowerGradient("FFR2AA1 _generator", -100.0, 200.0);
        input = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(timestamp1, raoInput1)), Set.of(powerGradientFFR1AA1, powerGradientFFR2AA1));
        parameters = new RaoParameters();

        createCoreProblemFillers();
        createPowerGradientConstraintFiller();
        buildAndFillLinearProblem();

        assertNotNull(linearProblem.getGeneratorPowerVariable("FFR1AA1 _load", timestamp1));
        assertNotNull(linearProblem.getGeneratorPowerVariable("FFR2AA1 _generator", timestamp1));
        assertThrows(OpenRaoException.class, () -> linearProblem.getGeneratorPowerVariable("FFR3AA1 _load", timestamp1));
        assertThrows(OpenRaoException.class, () -> linearProblem.getGeneratorPowerConstraint("FFR3AA1 _load", timestamp1));
    }

    @Test
    void testMissingGradientBound() throws IOException {
        createThreeTSInput();

        PowerGradient powerGradientFFR1AA1 = new PowerGradient("FFR1AA1 _load", null, 500.0);
        PowerGradient powerGradientFFR2AA1 = new PowerGradient("FFR2AA1 _generator", -100.0, null);
        PowerGradient powerGradientFFR3AA1 = new PowerGradient("FFR3AA1 _load", -500.0, 500.0);

        input = new InterTemporalRaoInput(new TemporalDataImpl<>(input.getRaoInputs().getDataPerTimestamp()), Set.of(powerGradientFFR1AA1, powerGradientFFR3AA1, powerGradientFFR2AA1));
        createCoreProblemFillers();
        createPowerGradientConstraintFiller();
        buildAndFillLinearProblem();

        OpenRaoMPConstraint powerGradientConstraintFR1TS12 = linearProblem.getGeneratorPowerGradientConstraint("FFR1AA1 _load", timestamp2, timestamp1);
        assertNotNull(powerGradientConstraintFR1TS12);
        assertEquals(-linearProblem.infinity(), powerGradientConstraintFR1TS12.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(500.0, powerGradientConstraintFR1TS12.ub());
        OpenRaoMPConstraint powerGradientConstraintFR2TS12 = linearProblem.getGeneratorPowerGradientConstraint("FFR2AA1 _generator", timestamp2, timestamp1);
        assertNotNull(powerGradientConstraintFR2TS12);
        assertEquals(-100.0, powerGradientConstraintFR2TS12.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), powerGradientConstraintFR2TS12.ub(), linearProblem.infinity() * 1e-3);
    }
}
