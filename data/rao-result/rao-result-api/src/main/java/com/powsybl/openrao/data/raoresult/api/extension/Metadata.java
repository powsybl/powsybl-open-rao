/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class Metadata extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "metadata";

    private final Map<State, ComputationStatus> computationStatusPerState;
    private String executionDetails;

    private Metadata(Map<State, ComputationStatus> computationStatusPerState, String executionDetails) {
        this.computationStatusPerState = new HashMap<>(computationStatusPerState);
        this.executionDetails = executionDetails;
    }

    /**
     * Returns the overall sensitivity computation status of the RAO.
     */
    public ComputationStatus getComputationStatus() {
        if (computationStatusPerState.isEmpty()) {
            return ComputationStatus.DEFAULT;
        }
        boolean anyFailure = false;
        for (State state : computationStatusPerState.keySet()) {
            ComputationStatus stateStatus = computationStatusPerState.get(state);
            if (stateStatus == ComputationStatus.FAILURE) {
                if (state.isPreventive()) {
                    return ComputationStatus.FAILURE;
                }
                anyFailure = true;
            }
        }
        return anyFailure ? ComputationStatus.PARTIAL_FAILURE : ComputationStatus.DEFAULT;
    }

    /**
     * Returns the sensitivity computation status for a given state.
     */
    public ComputationStatus getComputationStatus(State state) {
        return computationStatusPerState.getOrDefault(state, ComputationStatus.DEFAULT);
    }

    /**
     * Indicates which computation steps were executed by the RAO,
     * or any relevant information regarding the computation process.
     */
    public Optional<String> getExecutionDetails() {
        return Optional.ofNullable(executionDetails);
    }

    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    // builder

    public static MetadataBuilder build() {
        return new MetadataBuilder();
    }

    public static final class MetadataBuilder {
        private final Map<State, ComputationStatus> computationStatusPerState;
        private String executionDetails = null;

        private MetadataBuilder() {
            this.computationStatusPerState = new HashMap<>();
        }

        public MetadataBuilder addComputationStatus(State state, ComputationStatus computationStatus) {
            this.computationStatusPerState.put(state, computationStatus);
            return this;
        }

        public MetadataBuilder addExecutionDetails(String executionDetails) {
            this.executionDetails = executionDetails;
            return this;
        }

        public Metadata build() {
            return new Metadata(computationStatusPerState, executionDetails);
        }
    }
}
