/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FlowBasedMonitoredBranchResult {

    private final String id;
    private final String name;
    private final String branchId;
    private final double maximumFlow;
    private final List<FlowBasedBranchPtdfPerCountry> ptdfList;

    public FlowBasedMonitoredBranchResult(final String id,
                                           final String name,
                                           final String branchId,
                                           final double maximumFlow) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.branchId = Objects.requireNonNull(branchId);
        this.maximumFlow = maximumFlow;
        ptdfList = new ArrayList<>();
    }

    public FlowBasedMonitoredBranchResult(final String id,
                                           final String name,
                                           final String branchId,
                                           final double maximumFlow,
                                           final List<FlowBasedBranchPtdfPerCountry> ptdfList) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.branchId = Objects.requireNonNull(branchId);
        this.maximumFlow = maximumFlow;
        this.ptdfList = ptdfList;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FlowBasedBranchPtdfPerCountry> getPtdfList() {
        return ptdfList;
    }

}
