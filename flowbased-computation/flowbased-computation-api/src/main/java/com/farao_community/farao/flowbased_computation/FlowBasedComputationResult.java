/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPreContingency;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class FlowBasedComputationResult {

    public enum Status {
        FAILURE,
        SUCCESS
    }
    private final Status status;
    private final List<DataMonitoredBranch> ptdflist;

    public Status getStatus() {
        return status;
    }

    public FlowBasedComputationResult(final Status status) {
        this.status = status;
        this.ptdflist = new ArrayList<>();
    }

    public FlowBasedComputationResult(
            final Status status,
            final List<DataMonitoredBranch> ptdflist) {
        this.status = status;
        this.ptdflist = ptdflist;
    }

    public List<DataMonitoredBranch> getPtdflist() {
        return ptdflist;
    }

    public DataPreContingency createDataPreContingency() {
        return new DataPreContingency(ptdflist);
    }

    public DataDomain createDataDomain() {
        return new DataDomain("",
                "",
                "",
                "",
                createDataPreContingency());
    }

    public DataDomain createDataDomain(String id, String name, String sourceFormat, String description) {
        return new DataDomain(id,
                name,
                sourceFormat,
                description,
                createDataPreContingency());
    }

}
