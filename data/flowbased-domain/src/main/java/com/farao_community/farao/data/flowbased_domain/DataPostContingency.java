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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    public DataMonitoredBranch findMonitoredBranchById(String monitoredBranchId) {
        return dataMonitoredBranches.stream()
                .filter(dataMonitoredBranch -> dataMonitoredBranch.getId().equals(monitoredBranchId))
                .findAny()
                .orElse(null);
    }

    public void updateCurativeDataMonitoredBranches(List<DataMonitoredBranch> newData, String afterCraInstantId) {
        List<DataMonitoredBranch> newCurativeData = new ArrayList<>();
        newData.forEach(newBranch -> {
            if (newBranch.getInstantId().equals(afterCraInstantId)) {
                newCurativeData.add(newBranch);
            }
        });

        dataMonitoredBranches.forEach(dataMonitoredBranch -> {
            if (dataMonitoredBranch.getInstantId().equals(afterCraInstantId)) {
                List<DataMonitoredBranch> correspondingData = newCurativeData.stream().filter(newCurativeBranch ->
                    newCurativeBranch.correspondsTo(dataMonitoredBranch)).collect(Collectors.toList());
                if (correspondingData.size() == 1) {
                    dataMonitoredBranch.updateDataMonitoredBranch(correspondingData.get(0));
                } else {
                    throw new UnsupportedOperationException(String.format("Too many curative results for branch %s", dataMonitoredBranch.getBranchId()));
                }
            }
        });
    }
}
