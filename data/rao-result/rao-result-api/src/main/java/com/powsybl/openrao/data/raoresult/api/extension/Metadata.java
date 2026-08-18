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

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class Metadata extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "metadata";

    private ComputationStatus computationStatus;
    private final Map<State, ComputationStatus> computationStatusPerState;
    private String executionDetails;

    public Metadata() {
        this.computationStatus = ComputationStatus.DEFAULT;
        this.computationStatusPerState = new HashMap<>();
        this.executionDetails = "Not provided.";
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
        return computationStatus;
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
    public String getExecutionDetails() {
        return executionDetails;
    }

    void setComputationStatus(ComputationStatus computationStatus) {
        this.computationStatus = computationStatus;
    }

    void setComputationStatusPerState(State state, ComputationStatus computationStatus) {
        this.computationStatusPerState.put(state, computationStatus);
    }

    void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }
}
