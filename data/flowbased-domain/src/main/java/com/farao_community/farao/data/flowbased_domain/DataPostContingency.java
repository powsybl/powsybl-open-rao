/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
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
 * Business Object of the FlowBased DataPostContingency
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Builder
@Data
public class DataPostContingency {
    @NotNull(message = "contingency.id.empty")
    private String contingencyId;
    @NotNull(message = "contingency.dataMonitoredBranches.empty")
    @Valid
    private final List<DataMonitoredBranch> dataMonitoredBranches;

    @ConstructorProperties({"contingencyId", "dataMonitoredBranches"})
    public DataPostContingency(final String contingencyId, final List<DataMonitoredBranch> dataMonitoredBranches) {
        this.contingencyId = contingencyId;
        this.dataMonitoredBranches = Collections.unmodifiableList(dataMonitoredBranches);
    }

    public DataMonitoredBranch findMonitoredBranchByIdAndInstant(String monitoredBranchId, String instantId) {
        return dataMonitoredBranches.stream()
                .filter(dataMonitoredBranch -> dataMonitoredBranch.getId().equals(monitoredBranchId) && dataMonitoredBranch.getInstantId().equals(instantId))
                .findAny()
                .orElse(null);
    }
}
