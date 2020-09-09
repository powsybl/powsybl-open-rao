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
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class RaoInput {

    public static final class RaoInputBuilder {

        private Crac crac;
        private State optimizedState;
        private Network network;
        private String variantId;
        private List<Pair<Country, Country> > boundaries;
        private ReferenceProgram referenceProgram;

        private RaoInputBuilder() {

        }

        public RaoInputBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public RaoInputBuilder withOptimizedState(State state) {
            this.optimizedState = optimizedState;
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

        public RaoInputBuilder withBoundaries(List<Pair<Country, Country> > boundaries) {
            this.boundaries = boundaries;
            return this;
        }

        public RaoInputBuilder withRefProg(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
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
            raoInput.boundaries = boundaries;
            raoInput.referenceProgram = Objects.isNull(referenceProgram) ? Optional.empty() : Optional.of(referenceProgram);

            return raoInput;
        }
    }

    //TODO: add an optional GLSK provider argument
    private Crac crac;
    private State optimizedState;
    private Network network;
    private String variantId;
    private List<Pair<Country, Country> > boundaries;
    private Optional<ReferenceProgram> referenceProgram;

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
}
