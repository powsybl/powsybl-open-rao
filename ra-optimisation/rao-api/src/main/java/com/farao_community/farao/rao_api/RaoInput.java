/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class RaoInput {

    public static final class RaoInputBuilder {
        private static final String REQUIRED_ARGUMENT_MESSAGE = "%s is mandatory when building RAO input.";

        private Crac crac;
        private Network network;
        private String networkVariantId;
        private State optimizedState;
        private Set<State> perimeter;
        private ReferenceProgram referenceProgram;
        private ZonalData<SensitivityVariableSet> glsk;

        private RaoInputBuilder() {
        }

        public RaoInputBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public RaoInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public RaoInputBuilder withNetworkVariantId(String variantId) {
            this.networkVariantId = variantId;
            return this;
        }

        private RaoInputBuilder withOptimizedState(State state) {
            this.optimizedState = state;
            return this;
        }

        public RaoInputBuilder withPerimeter(Set<State> states) {
            this.perimeter = states;
            return this;
        }

        public RaoInputBuilder withRefProg(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
            return this;
        }

        public RaoInputBuilder withGlskProvider(ZonalData<SensitivityVariableSet> glsk) {
            this.glsk = glsk;
            return this;
        }

        public RaoInput build() {
            RaoInput raoInput = new RaoInput();
            raoInput.crac = Optional.ofNullable(crac).orElseThrow(() -> requiredArgumentError("CRAC"));

            raoInput.network = Optional.ofNullable(network).orElseThrow(() -> requiredArgumentError("Network"));
            raoInput.networkVariantId = networkVariantId != null ? networkVariantId : network.getVariantManager().getWorkingVariantId();
            raoInput.optimizedState = optimizedState;
            raoInput.perimeter = perimeter;
            raoInput.referenceProgram = referenceProgram;
            raoInput.glsk = glsk;
            return raoInput;
        }

        private RaoInputException requiredArgumentError(String type) {
            return new RaoInputException(format(REQUIRED_ARGUMENT_MESSAGE, type));
        }

    }

    private Crac crac;
    private State optimizedState;
    private Set<State> perimeter;
    private Network network;
    private String networkVariantId;
    private ReferenceProgram referenceProgram;
    private ZonalData<SensitivityVariableSet> glsk;

    private RaoInput() {
    }

    public static RaoInputBuilder build(Network network, Crac crac) {
        return new RaoInputBuilder().withNetwork(network).withCrac(crac);
    }

    public static RaoInputBuilder buildWithState(Network network, Crac crac, State optimizedState) {
        return build(network, crac).withOptimizedState(optimizedState).withPerimeter(Collections.singleton(optimizedState));
    }

    public static RaoInputBuilder buildWithPreventiveState(Network network, Crac crac) {
        return buildWithState(network, crac, crac.getPreventiveState());
    }

    public Crac getCrac() {
        return crac;
    }

    public State getOptimizedState() {
        return optimizedState;
    }

    public Set<State> getPerimeter() {
        return perimeter;
    }

    public Network getNetwork() {
        return network;
    }

    public String getNetworkVariantId() {
        return networkVariantId;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public void setReferenceProgram(ReferenceProgram referenceProgram) {
        Objects.requireNonNull(referenceProgram);
        this.referenceProgram = referenceProgram;
    }

    public ZonalData<SensitivityVariableSet> getGlskProvider() {
        return glsk;
    }
}
