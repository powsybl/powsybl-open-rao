/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
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
    void testNoUsageRulesShouldReturnUndefined() {
        State state = Mockito.mock(State.class);
        Set<UsageRule> usageRules = Collections.emptySet();
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnInstantUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(new OnInstantImpl(UsageMethod.AVAILABLE, instant));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, 0d, Collections.emptySet());
        assertEquals(UsageMethod.AVAILABLE, ra.getUsageMethod(state));
    }

    @Test
    void testStrongestInstantUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(
            new OnInstantImpl(UsageMethod.AVAILABLE, instant),
            new OnFlowConstraintInCountryImpl(UsageMethod.FORCED, instant, Optional.empty(), Country.FR));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnContingencyStateUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(new OnContingencyStateImpl(UsageMethod.AVAILABLE, state));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.AVAILABLE, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyStrongestStateUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();

        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.doReturn(state).when(flowCnec).getState();

        Set<UsageRule> usageRules = Set.of(
            new OnContingencyStateImpl(UsageMethod.AVAILABLE, state),
            new OnConstraintImpl<>(UsageMethod.FORCED, instant, flowCnec));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testStrongestStateAndInstantUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();

        Set<UsageRule> usageRules = Set.of(
            new OnContingencyStateImpl(UsageMethod.AVAILABLE, state),
            new OnInstantImpl(UsageMethod.FORCED, instant));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testDifferentInstantsBetweenOnFlowConstraintUsageRuleAndCnec() {
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);

        FlowCnec autoFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(autoFlowCnec.getState()).thenReturn(autoState);
        FlowCnec curativeFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(curativeFlowCnec.getState()).thenReturn(curativeState);

        Set<UsageRule> usageRules = Set.of(
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, autoFlowCnec),
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, curativeFlowCnec)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }

    @Test
    void testDifferentInstantsBetweenOnAngleConstraintUsageRuleAndCnec() {
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);

        AngleCnec autoAngleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(autoAngleCnec.getState()).thenReturn(autoState);
        AngleCnec curativeAngleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(curativeAngleCnec.getState()).thenReturn(curativeState);

        Set<UsageRule> usageRules = Set.of(
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, autoAngleCnec),
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, curativeAngleCnec)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }

    @Test
    void testDifferentInstantsBetweenOnVoltageConstraintUsageRuleAndCnec() {
        Instant autoInstant = Mockito.mock(Instant.class);
        Mockito.when(autoInstant.isPreventive()).thenReturn(false);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.isPreventive()).thenReturn(false);

        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);

        VoltageCnec autoVoltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(autoVoltageCnec.getState()).thenReturn(autoState);
        VoltageCnec curativeVoltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(curativeVoltageCnec.getState()).thenReturn(curativeState);

        Set<UsageRule> usageRules = Set.of(
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, autoVoltageCnec),
            new OnConstraintImpl<>(UsageMethod.FORCED, autoInstant, curativeVoltageCnec)
        );

        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0, null, Collections.emptySet());
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(autoState));
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(curativeState));
    }
}
