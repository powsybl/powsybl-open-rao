/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;

import java.util.Objects;
import java.util.Optional;

/**
 * This class represents the functional contingency scenario
 * It contains the auto and curative states that should be optimized after a given contingency
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class ContingencyScenario {
    private Contingency contingency;
    private State automatonState;
    private State curativeState;

    private ContingencyScenario() { }

    public static ContingencyScenarioBuilder create() {
        return new ContingencyScenarioBuilder();
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Optional<State> getAutomatonState() {
        return Objects.isNull(automatonState) ? Optional.empty() : Optional.of(automatonState);
    }

    public Optional<State> getCurativeState() {
        return Objects.isNull(curativeState) ? Optional.empty() : Optional.of(curativeState);
    }

    public static final class ContingencyScenarioBuilder {
        private Contingency contingency;
        private State automatonState;
        private State curativeState;

        private ContingencyScenarioBuilder() { }

        public ContingencyScenarioBuilder withContingency(Contingency contingency) {
            this.contingency = contingency;
            return this;
        }

        public ContingencyScenarioBuilder withAutomatonState(State automatonState) {
            this.automatonState = automatonState;
            return this;
        }

        public ContingencyScenarioBuilder withCurativeState(State curativeState) {
            this.curativeState = curativeState;
            return this;
        }

        public ContingencyScenario build() {
            Objects.requireNonNull(contingency);
            if (Objects.isNull(automatonState) && Objects.isNull(curativeState)) {
                throw new OpenRaoException(String.format("Contingency %s scenario should have at least an auto or curative state.", contingency.getId()));
            }
            checkStateContingencyAndInstant(automatonState, InstantKind.AUTO);
            checkStateContingencyAndInstant(curativeState, InstantKind.CURATIVE);
            ContingencyScenario contingencyScenario = new ContingencyScenario();
            contingencyScenario.contingency = contingency;
            contingencyScenario.automatonState = automatonState;
            contingencyScenario.curativeState = curativeState;
            return contingencyScenario;
        }

        private void checkStateContingencyAndInstant(State state, InstantKind instantKind) {
            if (Objects.nonNull(state)) {
                Optional<Contingency> stateContingency = state.getContingency();
                if (stateContingency.isEmpty() || !stateContingency.get().equals(contingency)) {
                    throw new OpenRaoException(String.format("State %s does not refer to the contingency %s.", state.getId(), contingency.getId()));
                }
                if (!instantKind.equals(state.getInstant().getKind())) {
                    throw new OpenRaoException(String.format("Instant of state %s is not of kind %s.", state.getId(), instantKind));
                }
            }
        }
    }
}
