/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.powsybl.commons.extensions.AbstractExtendable;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationResult extends AbstractExtendable<RaoComputationResult> {

    public enum Status {
        FAILURE,
        SUCCESS
    }

    private final Status status;
    private final PreContingencyResult preContingencyResult;
    private final List<ContingencyResult> contingencyResults;

    public RaoComputationResult(final Status status) {
        this.status = status;
        this.preContingencyResult = new PreContingencyResult();
        this.contingencyResults = new ArrayList<>();
    }

    public RaoComputationResult(final Status status, final PreContingencyResult preContingencyResult) {
        this.status = status;
        this.preContingencyResult = preContingencyResult;
        this.contingencyResults = new ArrayList<>();
    }

    @ConstructorProperties({"status", "preContingencyResult", "contingencyResults"})
    public RaoComputationResult(
            final Status status,
            final PreContingencyResult preContingencyResult,
            final List<ContingencyResult> contingencyResults) {
        this.status = status;
        this.preContingencyResult = preContingencyResult;
        this.contingencyResults = contingencyResults;
    }

    public Status getStatus() {
        return status;
    }

    public PreContingencyResult getPreContingencyResult() {
        return preContingencyResult;
    }

    public List<ContingencyResult> getContingencyResults() {
        return contingencyResults;
    }

    public void addContingencyResult(ContingencyResult contingencyResult) {
        contingencyResults.add(contingencyResult);
    }
}
