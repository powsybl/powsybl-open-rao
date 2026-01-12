/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class GeneratorConstraintsFillerTest {
    private final LinearProblemBuilder linearProblemBuilder = new LinearProblemBuilder().withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);
    private LinearProblem linearProblem;
    InterTemporalRaoInput input;
    RaoParameters parameters;

    private void createCoreProblemFillers() {
        input.getRaoInputs().getDataPerTimestamp().forEach((timestamp, raoInput) -> {
            Crac crac = raoInput.getCrac();
            OptimizationPerimeter optimizationPerimeter = new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                crac.getFlowCnecs(),
                Set.of(),
                crac.getNetworkActions(crac.getPreventiveState()),
                crac.getRangeActions(crac.getPreventiveState())
            );

            RangeActionsOptimizationParameters rangeActionParameters = parameters.getRangeActionsOptimizationParameters();
            Map<RangeAction<?>, Double> map = new HashMap<>();
            crac.getRangeActions(crac.getPreventiveState()).forEach(action -> map.put(action, 5000.0));
            RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(map);
            CostCoreProblemFiller coreProblemFiller = new CostCoreProblemFiller(
                optimizationPerimeter,
                rangeActionSetpointResult,
                rangeActionParameters,
                null,
                Unit.MEGAWATT,
                false,
                SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS,
                timestamp
            );
            linearProblemBuilder.withProblemFiller(coreProblemFiller);
        });
    }

    private void createPowerGradientConstraintFiller() {
        TemporalData<Network> networks = input.getRaoInputs().map(RaoInput::getNetwork);
        TemporalData<State> preventiveStates = input.getRaoInputs().map(RaoInput::getCrac).map(Crac::getPreventiveState).map(State.class::cast);
        TemporalData<Set<InjectionRangeAction>> injectionRangeActions = input.getRaoInputs().map(RaoInput::getCrac).map(crac -> crac.getRangeActions(crac.getPreventiveState()).stream().filter(InjectionRangeAction.class::isInstance).map(InjectionRangeAction.class::cast).collect(Collectors.toSet()));
        Set<GeneratorConstraints> generatorConstraints = input.getIntertemporalConstraints().getGeneratorConstraints();
        GeneratorConstraintsFiller generatorConstraintsFiller = new GeneratorConstraintsFiller(
            networks,
            preventiveStates,
            injectionRangeActions,
            generatorConstraints);
        linearProblemBuilder.withProblemFiller(generatorConstraintsFiller);
    }

    private void buildAndFillLinearProblem() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(flowResult.getFlow(any(), any(), any())).thenReturn(5000.0);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        linearProblem = linearProblemBuilder.build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void setUpLinearProblem() {
        createCoreProblemFillers();
        createPowerGradientConstraintFiller();
        buildAndFillLinearProblem();
    }

    private static Crac createSimpleRedispatchingCrac(OffsetDateTime timestamp, double cnecThreshold) {
        String cracId = "crac-" + timestamp.format(DateTimeFormatter.ISO_DATE_TIME);
        Crac crac = CracFactory.findDefault().create(cracId, cracId, timestamp);
        crac.newInstant("preventive", InstantKind.PREVENTIVE);
        crac.newFlowCnec()
            .withId("BE2 FR2 - preventive")
            .withNetworkElement("BBE2AA1  FFR2AA1  1")
            .withInstant("preventive")
            .withOptimized()
            .withNominalVoltage(380.0)
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMax(cnecThreshold)
            .withMin(-cnecThreshold)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        crac.newInjectionRangeAction()
            .withId("Redispatching BE-FR")
            .withName("Redispatching BE-FR")
            .withNetworkElementAndKey(-1.0, "FFR1AA1 _load")
            .withNetworkElementAndKey(1.0, "BBE1AA1 _generator")
            .newRange()
            .withMin(-3000.0)
            .withMax(3000.0)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();
        return crac;
    }

    private void setUpLinearProblemWithIntertemporalConstraints(IntertemporalConstraints intertemporalConstraints) {
        Network network = Network.read("6Nodes.xiidm", getClass().getResourceAsStream("/network/6Nodes.xiidm"));
        Map<OffsetDateTime, RaoInput> raoInputPerTimestamp = new HashMap<>();
        raoInputPerTimestamp.put(
            OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC),
            RaoInput.build(network, createSimpleRedispatchingCrac(OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC), 5000.0)).build()
        );
        raoInputPerTimestamp.put(
            OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC),
            RaoInput.build(network, createSimpleRedispatchingCrac(OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC), 5000.0)).build()
        );
        raoInputPerTimestamp.put(
            OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC),
            RaoInput.build(network, createSimpleRedispatchingCrac(OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC), 0.0)).build()
        );
        raoInputPerTimestamp.put(
            OffsetDateTime.of(2026, 1, 9, 3, 0, 0, 0, ZoneOffset.UTC),
            RaoInput.build(network, createSimpleRedispatchingCrac(OffsetDateTime.of(2026, 1, 9, 3, 0, 0, 0, ZoneOffset.UTC), 0.0)).build()
        );
        raoInputPerTimestamp.put(
            OffsetDateTime.of(2026, 1, 9, 4, 0, 0, 0, ZoneOffset.UTC),
            RaoInput.build(network, createSimpleRedispatchingCrac(OffsetDateTime.of(2026, 1, 9, 4, 0, 0, 0, ZoneOffset.UTC), 0.0)).build()
        );

        input = new InterTemporalRaoInput(new TemporalDataImpl<>(raoInputPerTimestamp), intertemporalConstraints);
        parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_minCost_megawatt_dc.json"));
        setUpLinearProblem();
    }

    @Test
    void testNoIntertemporalConstraints() {
        setUpLinearProblemWithIntertemporalConstraints(new IntertemporalConstraints());

        // For each timestamp:
        // -> no power variables created because no intertemporal constraints

        // - VARIABLES (20):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation

        // - CONSTRAINTS (15):
        //   - flow
        //   - set-point variation
        //   - network balancing

        assertEquals(20, linearProblem.numVariables());
        assertEquals(15, linearProblem.numConstraints());
    }

    @Test
    void testPMin() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (51):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (64):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(51, linearProblem.numVariables());
        assertEquals(64, linearProblem.numConstraints());

        checkInjectionKey();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
    }

    @Test
    void testPMax() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMax(5000.0).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (51):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - ON -> ON transition (except for last timestamp)

        // - CONSTRAINTS (64):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(34, linearProblem.numVariables());
        assertEquals(51, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
    }

    @Test
    void testPMinPMax() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (51):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (64):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(51, linearProblem.numVariables());
        assertEquals(64, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (51):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (64):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(51, linearProblem.numVariables());
        assertEquals(64, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLeadTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLeadTime(0.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (60):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (68):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(60, linearProblem.numVariables());
        assertEquals(68, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
    }

    @Test
    void testShortLagTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLagTime(0.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (60):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (72):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(60, linearProblem.numVariables());
        assertEquals(72, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testShortLeadAndShortLagTimes() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLeadTime(0.2).withLagTime(0.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (69):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (76):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(69, linearProblem.numVariables());
        assertEquals(76, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testShortLeadAndShortLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(0.2).withLagTime(0.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (69):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (76):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)

        assertEquals(69, linearProblem.numVariables());
        assertEquals(76, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testLongLeadTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLeadTime(1.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (64):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)

        // - CONSTRAINTS (72):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP UP (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - upward ramp (upper bound; except for last timestamp)

        assertEquals(64, linearProblem.numVariables());
        assertEquals(76, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
    }

    @Test
    void testLongLagTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLagTime(1.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (64):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)

        // - CONSTRAINTS (76):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - downward ramp (upper bound; except for last timestamp)

        assertEquals(64, linearProblem.numVariables());
        assertEquals(76, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testLongLeadAndLongLagTimes() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withLeadTime(1.2).withLagTime(1.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (77):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)

        // - CONSTRAINTS (88):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP UP (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - upward ramp (upper bound; except for last timestamp)
        //   - downward ramp (upper bound; except for last timestamp)

        assertEquals(77, linearProblem.numVariables());
        assertEquals(88, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testLongLeadAndLongLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(1.2).withLagTime(1.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (77):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)

        // - CONSTRAINTS (88):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP UP (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - upward ramp (upper bound; except for last timestamp)
        //   - downward ramp (upper bound; except for last timestamp)

        assertEquals(77, linearProblem.numVariables());
        assertEquals(88, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testLongLeadAndShortLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(1.2).withLagTime(0.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (73):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> OFF transition (except for last timestamp)
        //   - RAMP DOWN -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> RAMP UP transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)

        // - CONSTRAINTS (84):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP UP (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - upward ramp (upper bound; except for last timestamp)

        assertEquals(73, linearProblem.numVariables());
        assertEquals(84, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    @Test
    void testShortLeadAndLongLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withPMin(1000.0).withPMax(5000.0).withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(0.2).withLagTime(1.2).build());
        setUpLinearProblemWithIntertemporalConstraints(intertemporalConstraints);

        // For each timestamp:

        // - VARIABLES (73):
        //   - flow
        //   - redispatching set-point
        //   - upward set-point variation
        //   - downward set-point variation
        //   - generator power
        //   - ON state
        //   - OFF state
        //   - RAMP UP state
        //   - RAMP DOWN state
        //   - ON -> ON transition (except for last timestamp)
        //   - ON -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> RAMP DOWN transition (except for last timestamp)
        //   - RAMP DOWN -> OFF transition (except for last timestamp)
        //   - OFF -> OFF transition (except for last timestamp)
        //   - OFF -> ON transition (except for last timestamp)
        //   - RAMP UP -> ON transition (except for last timestamp)

        // - CONSTRAINTS (80):
        //   - flow
        //   - set-point variation
        //   - network balancing
        //   - generator power to redispatching
        //   - ON power (lower bound)
        //   - ON power (upper bound)
        //   - OFF power
        //   - only one state
        //   - state from ON (except for last timestamp)
        //   - state from OFF (except for last timestamp)
        //   - state from RAMP UP (except for last timestamp)
        //   - state from RAMP DOWN (except for last timestamp)
        //   - state to ON (except for last timestamp)
        //   - state to OFF (except for last timestamp)
        //   - state to RAMP DOWN (except for last timestamp)
        //   - power transition (lower bound; except for last timestamp)
        //   - power transition (upper bound; except for last timestamp)
        //   - downward ramp (upper bound; except for last timestamp)

        assertEquals(73, linearProblem.numVariables());
        assertEquals(80, linearProblem.numConstraints());

        checkInjectionKey();
        checkPMax();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_UP);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.RAMP_DOWN);
    }

    private void checkInjectionKey() {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC);
        InjectionRangeAction injectionRangeAction = input.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getInjectionRangeAction("Redispatching BE-FR");
        assertEquals(1.0, linearProblem.getGeneratorToInjectionConstraint("BBE1AA1 _generator", injectionRangeAction, timestamp).getCoefficient(linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", timestamp)));
    }

    private void checkPMax() {
        assertEquals(5000.0, linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC)).ub());
        assertEquals(5000.0, linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC)).ub());
        assertEquals(5000.0, linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC)).ub());
        assertEquals(5000.0, linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 3, 0, 0, 0, ZoneOffset.UTC)).ub());
        assertEquals(5000.0, linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 4, 0, 0, 0, ZoneOffset.UTC)).ub());
    }

    private void checkUpwardGradient() {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(1500.0, -linearProblem.getGeneratorPowerTransitionConstraint("BBE1AA1 _generator", timestamp, LinearProblem.AbsExtension.NEGATIVE).getCoefficient(linearProblem.getGeneratorStateTransitionVariable("BBE1AA1 _generator", timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON)));
    }

    private void checkDownwardGradient() {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(-1000.0, -linearProblem.getGeneratorPowerTransitionConstraint("BBE1AA1 _generator", timestamp, LinearProblem.AbsExtension.POSITIVE).getCoefficient(linearProblem.getGeneratorStateTransitionVariable("BBE1AA1 _generator", timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON)));
    }

    private void checkGeneratorStateVariableExists(LinearProblem.GeneratorState generatorState) {
        assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC), generatorState));
        assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 1, 0, 0, 0, ZoneOffset.UTC), generatorState));
        assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 2, 0, 0, 0, ZoneOffset.UTC), generatorState));
        assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 3, 0, 0, 0, ZoneOffset.UTC), generatorState));
        assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", OffsetDateTime.of(2026, 1, 9, 4, 0, 0, 0, ZoneOffset.UTC), generatorState));
    }
}
