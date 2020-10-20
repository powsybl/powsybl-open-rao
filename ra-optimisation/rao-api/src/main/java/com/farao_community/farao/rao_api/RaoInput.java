/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.powsybl.iidm.network.Network;

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
        private State optimizedState;
        private Set<State> perimeter;
        private Network network;
        private String variantId;
        private ReferenceProgram referenceProgram;
        private GlskProvider glskProvider;

        private RaoInputBuilder() {
        }

        public RaoInputBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public RaoInputBuilder withOptimizedState(State state) {
            this.optimizedState = state;
            return this;
        }

        public RaoInputBuilder withPerimeter(Set<State> states) {
            this.perimeter = states;
            return this;
        }

        public RaoInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public RaoInputBuilder withVariantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public RaoInputBuilder withRefProg(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
            return this;
        }

        public RaoInputBuilder withGlskProvider(GlskProvider glskProvider) {
            this.glskProvider = glskProvider;
            return this;
        }

        private RaoInputException requiredArgumentError(String type) {
            return new RaoInputException(format(REQUIRED_ARGUMENT_MESSAGE, type));
        }

        public RaoInput build() {
            RaoInput raoInput = new RaoInput();
            raoInput.crac = Optional.ofNullable(crac).orElseThrow(() -> requiredArgumentError("CRAC"));
            raoInput.network = Optional.ofNullable(network).orElseThrow(() -> requiredArgumentError("Network"));
            raoInput.optimizedState = Optional.ofNullable(optimizedState).orElseThrow(() -> requiredArgumentError("Optimized state"));
            raoInput.variantId = variantId != null ? variantId : network.getVariantManager().getWorkingVariantId();
            raoInput.perimeter = perimeter != null ? perimeter : Collections.singleton(raoInput.optimizedState);
            raoInput.referenceProgram = referenceProgram != null ? Optional.of(referenceProgram) : Optional.empty();
            raoInput.glskProvider = glskProvider != null ? Optional.of(glskProvider) : Optional.empty();
            return raoInput;
        }
    }

    private Crac crac;
    private State optimizedState;
    private Set<State> perimeter;
    private Network network;
    private String variantId;
    private Optional<ReferenceProgram> referenceProgram;
    private Optional<GlskProvider> glskProvider;

    private RaoInput() {
    }

    public static RaoInputBuilder builder() {
        return new RaoInputBuilder();
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

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public Optional<ReferenceProgram> getReferenceProgram() {
        return referenceProgram;
    }

    public void setReferenceProgram(ReferenceProgram referenceProgram) {
        Objects.requireNonNull(referenceProgram);
        this.referenceProgram = Optional.of(referenceProgram);
    }

    public Optional<GlskProvider> getGlskProvider() {
        return glskProvider;
    }
}
