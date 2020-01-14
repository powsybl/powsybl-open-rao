/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;

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

}
