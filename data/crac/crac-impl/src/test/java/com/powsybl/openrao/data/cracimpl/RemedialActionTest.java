/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class RemedialActionTest {
    @Test
    void testNoTriggerConditionsShouldReturnUndefined() {
        State state = Mockito.mock(State.class);
        Set<TriggerCondition> triggerConditions = Collections.emptySet();
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnInstantTriggerCondition() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<TriggerCondition> triggerConditions = Set.of(new TriggerConditionImpl(instant, null, null, null, UsageMethod.AVAILABLE));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.AVAILABLE, ra.getUsageMethod(state));
    }

    @Test
    void testStrongestInstantTriggerCondition() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(instant, null, null, null, UsageMethod.AVAILABLE),
            new TriggerConditionImpl(instant, null, null, Country.FR, UsageMethod.FORCED));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnContingencyStateTriggerCondition() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.of(contingency));
        Set<TriggerCondition> triggerConditions = Set.of(new TriggerConditionImpl(state.getInstant(), state.getContingency().get(), null, null, UsageMethod.AVAILABLE));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.AVAILABLE, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyStrongestStateTriggerCondition() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.of(contingency));

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.doReturn(state).when(flowCnec).getState();

        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(state.getInstant(), state.getContingency().get(), null, null, UsageMethod.AVAILABLE),
            new TriggerConditionImpl(instant, null, flowCnec, null, UsageMethod.FORCED));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testStrongestStateAndInstantTriggerCondition() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.of(contingency));

        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(state.getInstant(), state.getContingency().get(), null, null, UsageMethod.AVAILABLE),
            new TriggerConditionImpl(instant, null, null, null, UsageMethod.FORCED));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testDifferentInstantsBetweenOnFlowConstraintTriggerConditionAndCnec() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        Mockito.when(autoState.getContingency()).thenReturn(Optional.of(contingency));
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));

        FlowCnec autoFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(autoFlowCnec.getState()).thenReturn(autoState);
        FlowCnec curativeFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(curativeFlowCnec.getState()).thenReturn(curativeState);

        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(autoInstant, null, autoFlowCnec, null, UsageMethod.FORCED),
            new TriggerConditionImpl(autoInstant, null, curativeFlowCnec, null, UsageMethod.FORCED)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }

    @Test
    void testDifferentInstantsBetweenOnAngleConstraintTriggerConditionAndCnec() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        Mockito.when(autoState.getContingency()).thenReturn(Optional.of(contingency));
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));

        AngleCnec autoAngleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(autoAngleCnec.getState()).thenReturn(autoState);
        AngleCnec curativeAngleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(curativeAngleCnec.getState()).thenReturn(curativeState);

        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(autoInstant, null, autoAngleCnec, null, UsageMethod.FORCED),
            new TriggerConditionImpl(autoInstant, null, curativeAngleCnec, null, UsageMethod.FORCED)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }

    @Test
    void testDifferentInstantsBetweenOnVoltageConstraintTriggerConditionAndCnec() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        Mockito.when(autoState.getContingency()).thenReturn(Optional.of(contingency));
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(curativeState.getContingency()).thenReturn(Optional.of(contingency));

        VoltageCnec autoVoltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(autoVoltageCnec.getState()).thenReturn(autoState);
        VoltageCnec curativeVoltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(curativeVoltageCnec.getState()).thenReturn(curativeState);

        Set<TriggerCondition> triggerConditions = Set.of(
            new TriggerConditionImpl(autoInstant, null, autoVoltageCnec, null, UsageMethod.FORCED),
            new TriggerConditionImpl(autoInstant, null, curativeVoltageCnec, null, UsageMethod.FORCED)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", triggerConditions, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }
}
