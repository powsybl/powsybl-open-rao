/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class RaoInput {

    public static final class RaoInputBuilder {
        private Crac crac;
        private State optimizedState;
        private Set<State> perimeter;
        private Network network;
        private String variantId;
        private List<Pair<Country, Country>> boundaries;
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

        public RaoInputBuilder withBoundaries(List<Pair<Country, Country>> boundaries) {
            this.boundaries = boundaries;
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

        public RaoInput build() {
            if (Objects.isNull(network)) {
                throw new RaoInputException("Network is mandatory when building RAO input.");
            }
            if (Objects.isNull(crac)) {
                throw new RaoInputException("CRAC is mandatory when building RAO input.");
            }

            RaoInput raoInput = new RaoInput();
            raoInput.crac = crac;
            raoInput.network = network;
            if (Objects.isNull(variantId)) {
                raoInput.variantId = network.getVariantManager().getWorkingVariantId();
            } else {
                raoInput.variantId = variantId;
            }
            if (Objects.isNull(optimizedState)) {
                raoInput.optimizedState = crac.getPreventiveState();
            } else {
                raoInput.optimizedState = optimizedState;
            }
            if (Objects.isNull(perimeter)) {
                raoInput.perimeter = Collections.singleton(raoInput.optimizedState);
            } else {
                raoInput.perimeter = perimeter;
            }
            raoInput.boundaries = boundaries;
            raoInput.referenceProgram = Objects.isNull(referenceProgram) ? Optional.empty() : Optional.of(referenceProgram);
            raoInput.glskProvider = Objects.isNull(glskProvider) ? Optional.empty() : Optional.of(glskProvider);

            return raoInput;
        }
    }

    private Crac crac;
    private State optimizedState;
    private Set<State> perimeter;
    private Network network;
    private String variantId;
    private List<Pair<Country, Country>> boundaries;
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

    public List<Pair<Country, Country>> getBoundaries() {
        return boundaries;
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
