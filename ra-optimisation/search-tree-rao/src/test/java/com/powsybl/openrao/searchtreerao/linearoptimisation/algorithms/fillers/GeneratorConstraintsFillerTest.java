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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
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

    private void createGeneratorConstraintFiller() {
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
        createGeneratorConstraintFiller();
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
    void testNoLeadNoLag() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").build());
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
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).build());
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
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLeadTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLeadTime(0.2).build());
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
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLagTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLagTime(0.2).build());
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
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLeadAndShortLagTimes() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLeadTime(0.2).withLagTime(0.2).build());
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
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLeadAndShortLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(0.2).withLagTime(0.2).build());
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
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testLongLeadTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLeadTime(1.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lead time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(71, linearProblem.numConstraints());

        checkInjectionKey();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testLongLagTime() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLagTime(1.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lag time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(71, linearProblem.numConstraints());

        checkInjectionKey();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testLongLeadAndLongLagTimes() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withLeadTime(1.2).withLagTime(1.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lead time constraint
        //   - lag time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(78, linearProblem.numConstraints());

        checkInjectionKey();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testLongLeadAndLongLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(1.2).withLagTime(1.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lead time constraint
        //   - lag time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(78, linearProblem.numConstraints());

        checkInjectionKey();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testLongLeadAndShortLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(1.2).withLagTime(0.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lead time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(71, linearProblem.numConstraints());

        checkInjectionKey();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    @Test
    void testShortLeadAndLongLagTimesAndPowerGradients() {
        IntertemporalConstraints intertemporalConstraints = new IntertemporalConstraints();
        intertemporalConstraints.addGeneratorConstraints(GeneratorConstraints.create().withGeneratorId("BBE1AA1 _generator").withUpwardPowerGradient(1500.0).withDownwardPowerGradient(-1000.0).withLeadTime(0.2).withLagTime(1.2).build());
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

        // - CONSTRAINTS (71):
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
        //   - lag time constraint

        assertEquals(51, linearProblem.numVariables());
        assertEquals(71, linearProblem.numConstraints());

        checkInjectionKey();
        checkUpwardGradient();
        checkDownwardGradient();
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.ON);
        checkGeneratorStateVariableExists(LinearProblem.GeneratorState.OFF);
    }

    private void checkInjectionKey() {
        iterateOnTimestamps(timestamp -> assertEquals(1.0, linearProblem.getGeneratorToInjectionConstraint("BBE1AA1 _generator", input.getRaoInputs().getData(timestamp).orElseThrow().getCrac().getInjectionRangeAction("Redispatching BE-FR"), timestamp).getCoefficient(linearProblem.getGeneratorPowerVariable("BBE1AA1 _generator", timestamp))), 3);
    }

    private void checkUpwardGradient() {
        iterateOnTimestamps(timestamp -> assertEquals(1500.0, -linearProblem.getGeneratorPowerTransitionConstraint("BBE1AA1 _generator", timestamp, LinearProblem.AbsExtension.NEGATIVE).getCoefficient(linearProblem.getGeneratorStateTransitionVariable("BBE1AA1 _generator", timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON))), 3);
    }

    private void checkDownwardGradient() {
        iterateOnTimestamps(timestamp -> assertEquals(-1000.0, -linearProblem.getGeneratorPowerTransitionConstraint("BBE1AA1 _generator", timestamp, LinearProblem.AbsExtension.POSITIVE).getCoefficient(linearProblem.getGeneratorStateTransitionVariable("BBE1AA1 _generator", timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON))), 3);
    }

    private void checkGeneratorStateVariableExists(LinearProblem.GeneratorState generatorState) {
        iterateOnTimestamps(timestamp -> assertNotNull(linearProblem.getGeneratorStateVariable("BBE1AA1 _generator", timestamp, generatorState)), 4);
    }

    private static void iterateOnTimestamps(Consumer<OffsetDateTime> consumer, int lastHour) {
        for (int hour = 0; hour <= lastHour; hour++) {
            OffsetDateTime timestamp = OffsetDateTime.of(2026, 1, 9, hour, 0, 0, 0, ZoneOffset.UTC);
            consumer.accept(timestamp);
        }
    }
}
