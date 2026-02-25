/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;

import java.util.*;

/**
 * This class represents the functional contingency scenario
 * It contains the auto and curative states that should be optimized after a given contingency
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class ContingencyScenario {
    private Contingency contingency;
    private State automatonState;
    private List<Perimeter> curativePerimeters;

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

    public List<Perimeter> getCurativePerimeters() {
        return curativePerimeters;
    }

    public static final class ContingencyScenarioBuilder {
        private Contingency contingency;
        private State automatonState;
        private final Set<Perimeter> curativePerimeters = new HashSet<>();

        private ContingencyScenarioBuilder() { }

        public ContingencyScenarioBuilder withContingency(Contingency contingency) {
            this.contingency = contingency;
            return this;
        }

        public ContingencyScenarioBuilder withAutomatonState(State automatonState) {
            this.automatonState = automatonState;
            return this;
        }

        public ContingencyScenarioBuilder withCurativePerimeter(Perimeter curativePerimeter) {
            this.curativePerimeters.add(curativePerimeter);
            return this;
        }

        public ContingencyScenario build() {
            Objects.requireNonNull(contingency);
            if (Objects.isNull(automatonState) && curativePerimeters.isEmpty()) {
                throw new OpenRaoException(String.format("Contingency %s scenario should have at least an auto or curative state.", contingency.getId()));
            }
            checkStateContingencyAndInstant(automatonState, InstantKind.AUTO);
            curativePerimeters.forEach(curativePerimeter -> checkStateContingencyAndInstant(curativePerimeter.getRaOptimisationState(), InstantKind.CURATIVE));
            ContingencyScenario contingencyScenario = new ContingencyScenario();
            contingencyScenario.contingency = contingency;
            contingencyScenario.automatonState = automatonState;
            contingencyScenario.curativePerimeters = curativePerimeters.stream()
                .sorted(Comparator.comparingInt(perimeter -> perimeter.getRaOptimisationState().getInstant().getOrder()))
                .toList();
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
