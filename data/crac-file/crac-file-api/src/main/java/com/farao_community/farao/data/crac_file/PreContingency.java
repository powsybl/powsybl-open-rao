/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.List;

/**
 * Business object of pre-contingency inputs of CRAC file
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class PreContingency {

    @NotNull(message = "preContingency.monitoredBranches.empty")
    @Valid
    private final List<MonitoredBranch> monitoredBranches;

    @ConstructorProperties({"monitoredBranches"})
    public PreContingency(final List<MonitoredBranch> monitoredBranches) {
        this.monitoredBranches = monitoredBranches;
    }

}
