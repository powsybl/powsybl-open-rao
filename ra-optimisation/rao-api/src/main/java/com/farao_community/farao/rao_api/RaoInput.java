/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
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
        private String baseCracVariantId;
        private State optimizedState;
        private Set<State> perimeter;
        private Network network;
        private String networkVariantId;
        private ReferenceProgram referenceProgram;
        private GlskProvider glskProvider;

        private RaoInputBuilder() {
        }

        public RaoInputBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public RaoInputBuilder withBaseCracVariantId(String baseCracVariantId) {
            this.baseCracVariantId = baseCracVariantId;
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

        public RaoInputBuilder withNetworkVariantId(String variantId) {
            this.networkVariantId = variantId;
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
            RaoInput raoInput = new RaoInput();
            raoInput.crac = Optional.ofNullable(crac).orElseThrow(() -> requiredArgumentError("CRAC"));

            checkBaseCracVariantId();
            raoInput.baseCracVariantId = baseCracVariantId;

            raoInput.network = Optional.ofNullable(network).orElseThrow(() -> requiredArgumentError("Network"));
            raoInput.optimizedState = Optional.ofNullable(optimizedState).orElse(crac.getPreventiveState());
            raoInput.networkVariantId = networkVariantId != null ? networkVariantId : network.getVariantManager().getWorkingVariantId();
            raoInput.perimeter = perimeter != null ? perimeter : Collections.singleton(raoInput.optimizedState);
            raoInput.referenceProgram = referenceProgram;
            raoInput.glskProvider = glskProvider;
            return raoInput;
        }

        private RaoInputException requiredArgumentError(String type) {
            return new RaoInputException(format(REQUIRED_ARGUMENT_MESSAGE, type));
        }

        private void checkBaseCracVariantId() {
            ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
            if (baseCracVariantId != null) {
                if (resultVariantManager == null) {
                    throw new RaoInputException("Base CRAC variant cannot be specified if CRAC does not have result variant manager.");
                }
                if (!resultVariantManager.getVariants().contains(baseCracVariantId)) {
                    throw new RaoInputException(format("Base CRAC variant %s does not exist.", baseCracVariantId));
                }
            } else {
                if (resultVariantManager != null && resultVariantManager.getVariants() != null) {
                    throw new RaoInputException("Base CRAC variant has to be specified if CRAC already has got pre-optimization variant.");
                }
            }
        }
    }

    private Crac crac;
    private String baseCracVariantId;
    private State optimizedState;
    private Set<State> perimeter;
    private Network network;
    private String networkVariantId;
    private ReferenceProgram referenceProgram;
    private GlskProvider glskProvider;

    private RaoInput() {
    }

    public static RaoInputBuilder builder() {
        return new RaoInputBuilder();
    }

    public Crac getCrac() {
        return crac;
    }

    public String getBaseCracVariantId() {
        return baseCracVariantId;
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

    public GlskProvider getGlskProvider() {
        return glskProvider;
    }
}
