/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Data
public class FlowBasedComputationResult {

    public enum Status {
        FAILED,
        SUCCESS
    }

    private final Status status;
    private final List<FlowBasedMonitoredBranchResult> branchResultList;

    public FlowBasedComputationResult(Status status) {
        this.status = status;
        branchResultList = new ArrayList<>();
    }

    public List<FlowBasedMonitoredBranchResult> getBranchResultList() {
        return branchResultList;
    }

    public Status getStatus() {
        return status;
    }
}
