/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * A RaoData is an object that gathers Network, Crac and SystematicSensitivityResult data. It manages
 * variants of these objects to ensure data consistency at any moment. Network will remain the same at any moment
 * with no variant management. It is a single point of entry to manipulate all data related to linear rao with
 * variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoData {

    public static final class RaoDataBuilder {
        private Network network;
        private Crac crac;
        private State optimizedState;
        private Set<State> perimeter;
        private ReferenceProgram referenceProgram;
        private GlskProvider glskProvider;
        private boolean basedOnExistingVariant;

        private RaoDataVariantManager raoDataVariantManager;

        private RaoDataBuilder(Crac crac, String cracVariantId) {
            this.crac = crac;
            this.raoDataVariantManager = new RaoDataVariantManager(crac, cracVariantId);
            basedOnExistingVariant = true;
        }

        private RaoDataBuilder(Crac crac) {
            this.crac = crac;
            this.raoDataVariantManager = new RaoDataVariantManager(crac);
            basedOnExistingVariant = false;
        }

        public RaoDataBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public RaoDataBuilder withOptimizedState(State state) {
            this.optimizedState = state;
            return this;
        }

        public RaoDataBuilder withPerimeter(Set<State> perimeter) {
            this.perimeter = perimeter;
            return this;
        }

        public RaoDataBuilder withGlskProvider(GlskProvider glskProvider) {
            this.glskProvider = glskProvider;
            return this;
        }

        public RaoDataBuilder withReferenceProgram(ReferenceProgram referenceProgram) {
            this.referenceProgram = referenceProgram;
            return this;
        }

        public RaoDataBuilder withRaoInput(RaoInput raoInput) {
            network = raoInput.getNetwork();
            optimizedState = raoInput.getOptimizedState();
            perimeter = raoInput.getPerimeter();
            glskProvider = raoInput.getGlskProvider();
            referenceProgram = raoInput.getReferenceProgram();
            return this;
        }

        public RaoDataBuilder withRaoData(RaoData raoData) {
            optimizedState = raoData.getOptimizedState();
            perimeter = raoData.getPerimeter();
            glskProvider = raoData.getGlskProvider();
            referenceProgram = raoData.getReferenceProgram();
            return this;
        }

        public RaoData build() {
            RaoData raoData = new RaoData();
            raoData.crac = crac;
            raoData.network = Optional.ofNullable(network).orElseThrow(() -> new FaraoException("Unable to build RAO data without network."));
            raoData.optimizedState = Optional.ofNullable(optimizedState).orElseThrow(() -> new FaraoException("Unable to build RAO data without optimized state."));
            raoData.perimeter = Optional.ofNullable(perimeter).orElseThrow(() -> new FaraoException("Unable to build RAO data without perimeter."));
            raoData.glskProvider = glskProvider;
            raoData.referenceProgram = referenceProgram;

            raoData.raoDataVariantManager = raoDataVariantManager;
            raoData.raoDataManager = new RaoDataManager(raoData);
            if (!basedOnExistingVariant) {
                raoData.raoDataManager.fillRangeActionResultsWithNetworkValues();
            }
            return raoData;
        }
    }

    private Network network;
    private Crac crac;
    private State optimizedState;
    private Set<State> perimeter;
    private ReferenceProgram referenceProgram;
    private GlskProvider glskProvider;

    private RaoDataVariantManager raoDataVariantManager;
    private RaoDataManager raoDataManager;

    private RaoData() {

    }

    public static RaoData fromNetworkAndPreviousRaoData(Network network, RaoData raoData) {
        return RaoData.builderFromCrac(raoData.getCrac())
            .withNetwork(network)
            .withOptimizedState(raoData.optimizedState)
            .withPerimeter(raoData.perimeter)
            .withReferenceProgram(raoData.referenceProgram)
            .withGlskProvider(raoData.glskProvider)
            .build();
    }

    public static RaoDataBuilder builderFromExistingCracVariant(Crac crac, String cracVariantId) {
        return new RaoDataBuilder(crac, cracVariantId);
    }

    public static RaoDataBuilder builderFromCrac(Crac crac) {
        return new RaoDataBuilder(crac);
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public GlskProvider getGlskProvider() {
        return glskProvider;
    }

    public Set<Cnec> getCnecs() {
        Set<Cnec> cnecs = new HashSet<>();
        perimeter.forEach(state -> cnecs.addAll(crac.getCnecs(state)));
        return cnecs;
    }

    public Set<RangeAction> getAvailableRangeActions() {
        return crac.getRangeActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return crac.getNetworkActions(network, optimizedState, UsageMethod.AVAILABLE);
    }

    public State getOptimizedState() {
        return optimizedState;
    }

    public Set<State> getPerimeter() {
        return perimeter;
    }

    public RaoDataManager getRaoDataManager() {
        return raoDataManager;
    }

    public RaoDataVariantManager getVariantManager() {
        return raoDataVariantManager;
    }

    // Delegate methods of RaoDataVariantManager
    public String getWorkingVariantId() {
        return getVariantManager().getWorkingVariantId();
    }

    public String getInitialVariantId() {
        return getVariantManager().getInitialVariantId();
    }

    public CracResult getCracResult(String variantId) {
        return getVariantManager().getCracResult(variantId);
    }

    public CracResult getCracResult() {
        return getVariantManager().getCracResult();
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        return getVariantManager().getSystematicSensitivityResult();
    }

    public void setSystematicSensitivityResult(SystematicSensitivityResult systematicSensitivityResult) {
        getVariantManager().setSystematicSensitivityResult(systematicSensitivityResult);
    }

    public boolean hasSensitivityValues() {
        return getSystematicSensitivityResult() != null;
    }

    public double getReferenceFlow(Cnec cnec) {
        return getSystematicSensitivityResult().getReferenceFlow(cnec);
    }

    public double getSensitivity(Cnec cnec, RangeAction rangeAction) {
        return getSystematicSensitivityResult().getSensitivityOnFlow(rangeAction, cnec);
    }
}
