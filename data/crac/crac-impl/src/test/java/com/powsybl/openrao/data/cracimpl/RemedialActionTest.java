/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
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
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
        assertEquals(UsageMethod.UNDEFINED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnInstantUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(new OnInstantImpl(UsageMethod.AVAILABLE, instant));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
        assertEquals(UsageMethod.AVAILABLE, ra.getUsageMethod(state));
    }

    @Test
    void testStrongestInstantUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(
            new OnInstantImpl(UsageMethod.AVAILABLE, instant),
            new OnFlowConstraintInCountryImpl(UsageMethod.FORCED, instant, Country.FR));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }

    @Test
    void testOnlyOneOnContingencyStateUsageRule() {
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.doReturn(false).when(instant).isPreventive();
        Mockito.doReturn(instant).when(state).getInstant();
        Set<UsageRule> usageRules = Set.of(new OnContingencyStateImpl(UsageMethod.AVAILABLE, state));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
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
            new OnFlowConstraintImpl(UsageMethod.FORCED, instant, flowCnec));
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
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
        AbstractRemedialAction<?> ra = new NetworkActionImpl("id", "name", "operator", usageRules, Collections.emptySet(), 0);
        assertEquals(UsageMethod.FORCED, ra.getUsageMethod(state));
    }
}
