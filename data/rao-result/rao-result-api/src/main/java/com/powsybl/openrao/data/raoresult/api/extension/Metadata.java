/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class Metadata extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "metadata";

    private final Map<State, ComputationStatus> computationStatusPerState;
    private String executionDetails;

    public Metadata() {
        this.computationStatusPerState = new HashMap<>();
        this.executionDetails = null;
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
                    // TODO: is it okay in multi-timestamp computations?
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
                Optional<Contingency> contingency = state.getContingency();
                if (contingency.isPresent()) {
                    jsonGenerator.writeStringField("contingency", contingency.get().getId());
                }
                Optional<OffsetDateTime> timestamp = state.getTimestamp();
                if (timestamp.isPresent()) {
                    jsonGenerator.writeStringField("timestamp", DateTimeFormatter.ISO_DATE_TIME.format(timestamp.get()));
                }
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
