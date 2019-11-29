/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.PreContingency;

import java.util.List;

public class PreContingencyValidation {
    /**
     * Methode for specify how information if you want set in PreContingency object
     * @param monitoredBranches
     */
    public PreContingency preContingencyValidation(List<MonitoredBranch> monitoredBranches) {
        return PreContingency.builder()
                .monitoredBranches(monitoredBranches)
                .build();

    }
}
