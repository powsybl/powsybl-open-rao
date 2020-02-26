/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.farao_community.farao.commons.FaraoException;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PreContingencyResult {
    private final List<MonitoredBranchResult> monitoredBranchResults;
    private final List<RemedialActionResult> remedialActionResults;

    public PreContingencyResult() {
        this.monitoredBranchResults = new ArrayList<>();
        this.remedialActionResults = new ArrayList<>();
    }

    public PreContingencyResult(
            final List<MonitoredBranchResult> monitoredBranchResults) {
        this.monitoredBranchResults = monitoredBranchResults;
        this.remedialActionResults = new ArrayList<>();
    }

    @ConstructorProperties({"monitoredBranchResults", "remedialActionResults"})
    public PreContingencyResult(
            final List<MonitoredBranchResult> monitoredBranchResults,
            final List<RemedialActionResult> remedialActionResults) {
        this.monitoredBranchResults = monitoredBranchResults;
        this.remedialActionResults = remedialActionResults;
    }

    public List<MonitoredBranchResult> getMonitoredBranchResults() {
        return monitoredBranchResults;
    }

    public List<RemedialActionResult> getRemedialActionResults() {
        return remedialActionResults;
    }

    public RemedialActionResult getRemedialActionResultById(String remedialActionResultId) {
        Optional<RemedialActionResult> optionalRemedialActionResult = getRemedialActionResults().stream()
                .filter(remedialActionResult -> remedialActionResult.getId().equals(remedialActionResultId)).findFirst();
        if (optionalRemedialActionResult.isPresent()) {
            return optionalRemedialActionResult.get();
        } else {
            throw new FaraoException(String.format("No result found for Remedial Action %s", remedialActionResultId));
        }
    }

}
