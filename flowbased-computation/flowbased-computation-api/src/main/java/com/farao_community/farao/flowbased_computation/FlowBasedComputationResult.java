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
 * FlowBased Computation Result
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class FlowBasedComputationResult {

    /**
     * FlowBased Computation status
     */
    public enum Status {
        FAILURE,
        SUCCESS
    }

    /**
     * status
     */
    private final Status status;

    /**
     * List of DataMonitoredBranch
     */
    private final List<DataMonitoredBranch> ptdflist;

    /**
     * Constructor
     * @param status status
     */
    public FlowBasedComputationResult(final Status status) {
        this.status = status;
        this.ptdflist = new ArrayList<>();
    }

    /**
     * @return get list of DataMonitoredBranch
     */
    public List<DataMonitoredBranch> getPtdflist() {
        return ptdflist;
    }

    /**
     * @return create DataPreContingency object from ptdflist
     */
    public DataPreContingency createDataPreContingency() {
        return new DataPreContingency(ptdflist);
    }

    /**
     * @return create empty DataDomain
     */
    public DataDomain createDataDomain() {
        return new DataDomain("",
                "",
                "",
                "",
                createDataPreContingency());
    }

    /**
     * @return get status
     */
    public Status getStatus() {
        return status;
    }

}
