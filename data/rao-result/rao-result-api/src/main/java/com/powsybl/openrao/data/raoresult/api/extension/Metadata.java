/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class Metadata extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "metadata";
    private static final ComputationStatus DEFAULT_COMPUTATION_STATUS = ComputationStatus.DEFAULT;

    private ComputationStatus computationStatus;
    private final Map<State, ComputationStatus> computationStatusPerState;
    private String executionDetails;

    public Metadata() {
        this.computationStatus = DEFAULT_COMPUTATION_STATUS;
        this.computationStatusPerState = new HashMap<>();
        this.executionDetails = null;
    }

    /**
     * Returns the overall sensitivity computation status of the RAO.
     */
    public ComputationStatus getComputationStatus() {
        // Currently used as an attribute.
        // Should be computed from the states as follows:
        // - preventive failure -> failure
        // - preventive default + not all curative failure -> partial failure
        // - preventive default + all curative failure -> failure
        // - preventive default + all curative default -> default
        // if (computationStatusPerState.isEmpty()) {
        //     return DEFAULT_COMPUTATION_STATUS;
        // }
        // boolean anyFailure = false;
        // for (State state : computationStatusPerState.keySet()) {
        //     ComputationStatus stateStatus = computationStatusPerState.get(state);
        //     if (stateStatus == ComputationStatus.FAILURE) {
        //         if (state.isPreventive()) {
        //             // TODO: is it okay in multi-timestamp computations?
        //             return ComputationStatus.FAILURE;
        //         }
        //         anyFailure = true;
        //     }
        // }
        // return anyFailure ? ComputationStatus.PARTIAL_FAILURE : ComputationStatus.DEFAULT;
        return computationStatus == null ? DEFAULT_COMPUTATION_STATUS : computationStatus;
    }

    /**
     * Returns the sensitivity computation status for a given state.
     */
    public ComputationStatus getComputationStatus(State state) {
        return computationStatusPerState.getOrDefault(state, DEFAULT_COMPUTATION_STATUS);
    }

    /**
     * Indicates which computation steps were executed by the RAO,
     * or any relevant information regarding the computation process.
     */
    public Optional<String> getExecutionDetails() {
        return Optional.ofNullable(executionDetails);
    }

    public void setComputationStatus(ComputationStatus computationStatus) {
        if (computationStatus != null) {
            this.computationStatus = computationStatus;
        }
    }

    public void setComputationStatus(State state, ComputationStatus computationStatus) {
        if (computationStatus != null) {
            this.computationStatusPerState.put(state, computationStatus);
        }
    }

    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    public void serialize(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("computationStatus", getComputationStatusAsString(getComputationStatus()));
        if (executionDetails != null) {
            jsonGenerator.writeStringField("executionDetails", executionDetails);
        }
        if (!computationStatusPerState.isEmpty()) {
            jsonGenerator.writeArrayFieldStart("computationStatusMap");
            for (State state : getSortedStates()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("computationStatus", getComputationStatusAsString(computationStatusPerState.get(state)));
                jsonGenerator.writeStringField("instant", state.getInstant().getId());
                if (state.getContingency().isPresent()) {
                    jsonGenerator.writeStringField("contingency", state.getContingency().get().getId());
                }
                // TODO: serialize timestamp?
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }

    private List<State> getSortedStates() {
        return computationStatusPerState.keySet().stream().sorted().toList();
    }

    private static String getComputationStatusAsString(ComputationStatus computationStatus) {
        return computationStatus.name().toLowerCase().replace("_", "-");
    }
}
