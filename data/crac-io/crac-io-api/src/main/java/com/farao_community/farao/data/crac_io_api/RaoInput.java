/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
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

    public static class Builder {

        private Crac crac;
        private Network network;
        private String variantId;
        private List<Pair<Country, Country> > boundaries;
        private ReferenceProgram referenceProgram;

        public Builder newRaoInput() {
            crac = null;
            network = null;
            variantId = null;
            boundaries = null;

            return this;
        }

        public Builder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public Builder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public Builder withVariantId(String variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder withBoundaries(List<Pair<Country, Country> > boundaries) {
            this.boundaries = boundaries;
            return this;
        }

        public Builder withRefProg(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
            return this;
        }

        public RaoInput build() {
            RaoInput raoInput = new RaoInput();
            raoInput.crac = crac;
            raoInput.network = network;
            raoInput.variantId = variantId;
            raoInput.boundaries = boundaries;
            raoInput.referenceProgram = Objects.isNull(referenceProgram) ? Optional.empty() : Optional.of(referenceProgram);

            return raoInput;
        }
    }

    //TODO: add an optional GLSK provider argument
    private Crac crac;
    private Network network;
    private String variantId;
    private List<Pair<Country, Country> > boundaries;
    private Optional<ReferenceProgram> referenceProgram;

    private RaoInput() {
    }

    public Crac getCrac() {
        return crac;
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
