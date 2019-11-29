/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

/**
 * Business object of a monitored branch in CRAC file
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class MonitoredBranch {
    @NotNull(message = "monitoredBranch.id.empty")
    protected final String id;
    @NotNull(message = "monitoredBranch.name.empty")
    protected final String name;
    @NotNull(message = "monitoredBranch.branchId.empty")
    protected final String branchId;
    protected final double fmax;

    @ConstructorProperties({"id", "name", "branchId", "fmax"})
    public MonitoredBranch(String id, String name, String branchId, double fmax) {
        this.id = id;
        this.name = name;
        this.branchId = branchId;
        this.fmax = fmax;
    }
}
