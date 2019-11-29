/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.converter.ActivationConverter;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Business object for a contingency in the CRAC file
 */
@Builder
@Getter
public class ContingencyXlsx {
    @ExcelColumn(position = 0)
    private final String uniqueCOName;
    @ExcelColumn(position = 1, convertorClass = ActivationConverter.class)
    private final Activation activation;
    private final List<MonitoredBranchXlsx> monitoredBranches;
    private final List<ContingencyElementXlsx> contingencyElements;

    public ContingencyXlsx() {
        this(null, null, null, null);
    }

    public ContingencyXlsx(final String uniqueCOName, final Activation activation, final List<MonitoredBranchXlsx> monitoredBranches,
                           final List<ContingencyElementXlsx> contingencyElements) {
        this.uniqueCOName = uniqueCOName;
        this.activation = activation;
        this.monitoredBranches = monitoredBranches;
        this.contingencyElements = contingencyElements;
    }

    @Override
    public String toString() {
        return this.uniqueCOName;
    }
}
