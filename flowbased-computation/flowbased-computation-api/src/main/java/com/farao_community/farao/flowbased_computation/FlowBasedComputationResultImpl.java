/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.flowbased_domain.DataDomain;

/**
 * FlowBased Computation Result
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationResultImpl implements FlowBasedComputationResult {
    private final Status status;
    private final DataDomain flowBasedDomain;

    public FlowBasedComputationResultImpl() {
        status = null;
        flowBasedDomain = null;
    }

    public FlowBasedComputationResultImpl(final Status status, final DataDomain flowBasedDomain) {
        this.status = status;
        this.flowBasedDomain = flowBasedDomain;
    }

    public DataDomain getFlowBasedDomain() {
        return flowBasedDomain;
    }

    public Status getStatus() {
        return status;
    }

}
