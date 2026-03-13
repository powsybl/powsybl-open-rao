/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class UsageRuleTest {
    private final Instant preventiveInstant = new InstantTest.InstantImplTest(0, InstantKind.PREVENTIVE);
    private final Instant curativeInstant = new InstantTest.InstantImplTest(1, InstantKind.CURATIVE);
    private final Contingency contingency1 = new Contingency("contingency-1", "contingency-1", List.of());
    private final Contingency contingency2 = new Contingency("contingency-2", "contingency-2", List.of());
    private final State preventiveState = new MockState(preventiveInstant, null, null);
    private final State curativeState1 = new MockState(curativeInstant, contingency1, null);
    private final State curativeState2 = new MockState(curativeInstant, contingency2, null);

    // mock classes for usage rules

    public static class OnInstantMock implements OnInstant {
        private final Instant instant;

        OnInstantMock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant getInstant() {
            return instant;
        }
    }

    public static class OnContingencyStateMock implements OnContingencyState {
        private final State state;

        OnContingencyStateMock(State state) {
            this.state = state;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public Contingency getContingency() {
            return state.getContingency().get();
        }

        @Override
        public Instant getInstant() {
            return state.getInstant();
        }
    }

    public static class OnConstraintMock implements OnConstraint<FlowCnec> {
        private final Instant instant;
        private final FlowCnec flowCnec;

        OnConstraintMock(Instant instant, FlowCnec flowCnec) {
            this.instant = instant;
            this.flowCnec = flowCnec;
        }

        @Override
        public FlowCnec getCnec() {
            return flowCnec;
        }

        @Override
        public Instant getInstant() {
            return instant;
        }
    }

    public static class OnFlowConstraintInCountryMock implements OnFlowConstraintInCountry {
        private final Instant instant;
        private final Country country;
        private final Contingency contingency;

        OnFlowConstraintInCountryMock(Instant instant, Country country, Contingency contingency) {
            this.instant = instant;
            this.country = country;
            this.contingency = contingency;
        }

        @Override
        public Country getCountry() {
            return country;
        }

        @Override
        public Instant getInstant() {
            return instant;
        }

        @Override
        public Optional<Contingency> getContingency() {
            return Optional.ofNullable(contingency);
        }
    }

    @Test
    void testOnInstantDefinedForState() {
        OnInstant preventiveOnInstant = new OnInstantMock(preventiveInstant);
        assertTrue(preventiveOnInstant.isDefinedForState(preventiveState));
        assertFalse(preventiveOnInstant.isDefinedForState(curativeState1));
        assertFalse(preventiveOnInstant.isDefinedForState(curativeState2));

        OnInstant curativeOnInstant = new OnInstantMock(curativeInstant);
        assertFalse(curativeOnInstant.isDefinedForState(preventiveState));
        assertTrue(curativeOnInstant.isDefinedForState(curativeState1));
        assertTrue(curativeOnInstant.isDefinedForState(curativeState2));
    }

    @Test
    void testOnContingencyStateDefinedForState() {
        OnContingencyState preventiveOnContingencyState = new OnContingencyStateMock(preventiveState);
        assertTrue(preventiveOnContingencyState.isDefinedForState(preventiveState));
        assertFalse(preventiveOnContingencyState.isDefinedForState(curativeState1));
        assertFalse(preventiveOnContingencyState.isDefinedForState(curativeState2));

        OnContingencyState curativeOnContingencyState = new OnContingencyStateMock(curativeState1);
        assertFalse(curativeOnContingencyState.isDefinedForState(preventiveState));
        assertTrue(curativeOnContingencyState.isDefinedForState(curativeState1));
        assertFalse(curativeOnContingencyState.isDefinedForState(curativeState2));
    }

    @Test
    void testOnConstraintDefinedForState() {
        FlowCnec preventiveFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(preventiveFlowCnec.getState()).thenReturn(preventiveState);

        FlowCnec curativeFlowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(curativeFlowCnec.getState()).thenReturn(curativeState1);

        OnConstraint<FlowCnec> preventiveOnConstraintPreventiveCnec = new OnConstraintMock(preventiveInstant, preventiveFlowCnec);
        assertTrue(preventiveOnConstraintPreventiveCnec.isDefinedForState(preventiveState));
        assertFalse(preventiveOnConstraintPreventiveCnec.isDefinedForState(curativeState1));
        assertFalse(preventiveOnConstraintPreventiveCnec.isDefinedForState(curativeState2));

        OnConstraint<FlowCnec> preventiveOnConstraintCurativeCnec = new OnConstraintMock(preventiveInstant, curativeFlowCnec);
        assertTrue(preventiveOnConstraintCurativeCnec.isDefinedForState(preventiveState));
        assertFalse(preventiveOnConstraintCurativeCnec.isDefinedForState(curativeState1));
        assertFalse(preventiveOnConstraintCurativeCnec.isDefinedForState(curativeState2));

        OnConstraint<FlowCnec> curativeOnConstraintCurativeCnec = new OnConstraintMock(curativeInstant, curativeFlowCnec);
        assertFalse(curativeOnConstraintCurativeCnec.isDefinedForState(preventiveState));
        assertTrue(curativeOnConstraintCurativeCnec.isDefinedForState(curativeState1));
        assertFalse(curativeOnConstraintCurativeCnec.isDefinedForState(curativeState2));
    }

    @Test
    void testOnFlowConstraintInCountryDefinedForState() {
        OnFlowConstraintInCountry preventiveOnFlowConstraintInCountry = new OnFlowConstraintInCountryMock(preventiveInstant, Country.FR, null);
        assertTrue(preventiveOnFlowConstraintInCountry.isDefinedForState(preventiveState));
        assertFalse(preventiveOnFlowConstraintInCountry.isDefinedForState(curativeState1));
        assertFalse(preventiveOnFlowConstraintInCountry.isDefinedForState(curativeState2));

        OnFlowConstraintInCountry curativeOnFlowConstraintInCountryWithoutContingency = new OnFlowConstraintInCountryMock(curativeInstant, Country.FR, null);
        assertFalse(curativeOnFlowConstraintInCountryWithoutContingency.isDefinedForState(preventiveState));
        assertTrue(curativeOnFlowConstraintInCountryWithoutContingency.isDefinedForState(curativeState1));
        assertTrue(curativeOnFlowConstraintInCountryWithoutContingency.isDefinedForState(curativeState2));

        OnFlowConstraintInCountry curativeOnFlowConstraintInCountryWithContingency = new OnFlowConstraintInCountryMock(curativeInstant, Country.FR, contingency1);
        assertFalse(curativeOnFlowConstraintInCountryWithContingency.isDefinedForState(preventiveState));
        assertTrue(curativeOnFlowConstraintInCountryWithContingency.isDefinedForState(curativeState1));
        assertFalse(curativeOnFlowConstraintInCountryWithContingency.isDefinedForState(curativeState2));
    }
}
