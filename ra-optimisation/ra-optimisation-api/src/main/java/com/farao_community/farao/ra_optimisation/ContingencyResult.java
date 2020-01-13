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
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ContingencyResult {
    private final String id;
    private final String name;
    private final List<MonitoredBranchResult> monitoredBranchResults;
    private final List<RemedialActionResult> remedialActionResults;

    public ContingencyResult(final String id,
                             final String name) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.monitoredBranchResults = new ArrayList<>();
        this.remedialActionResults = new ArrayList<>();
    }

    public ContingencyResult(final String id,
                             final String name,
                             final List<MonitoredBranchResult> monitoredBranchResults) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.monitoredBranchResults = monitoredBranchResults;
        this.remedialActionResults = new ArrayList<>();
    }

    @ConstructorProperties({"id", "name", "monitoredBranchResults", "remedialActionResults"})
    public ContingencyResult(final String id,
                             final String name,
                             final List<MonitoredBranchResult> monitoredBranchResults,
                             final List<RemedialActionResult> remedialActionResults) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.monitoredBranchResults = monitoredBranchResults;
        this.remedialActionResults = remedialActionResults;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MonitoredBranchResult> getMonitoredBranchResults() {
        return monitoredBranchResults;
    }

    public List<RemedialActionResult> getRemedialActionResults() {
        return remedialActionResults;
    }

}
