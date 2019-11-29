/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

/**
 * Business Object of the FlowBased DataPreContingency
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class DataPreContingency {
    @NotNull(message = "dataMonitoredBranches.empty")
    @Valid
    private final List<DataMonitoredBranch> dataMonitoredBranches;

    @ConstructorProperties("dataMonitoredBranches")
    public DataPreContingency(final List<DataMonitoredBranch> dataMonitoredBranches) {
        this.dataMonitoredBranches = Collections.unmodifiableList(dataMonitoredBranches);
    }

    public DataMonitoredBranch findMonitoredBranchbyId(String monitoredBranchId) {
        return dataMonitoredBranches.stream()
                .filter(dataMonitoredBranch -> dataMonitoredBranch.getId().equals(monitoredBranchId))
                .findAny()
                .orElse(null);
    }
}
