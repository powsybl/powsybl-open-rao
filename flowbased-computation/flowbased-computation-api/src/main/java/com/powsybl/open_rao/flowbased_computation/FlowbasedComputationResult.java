/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.flowbased_computation;

import com.powsybl.open_rao.data.flowbased_domain.DataDomain;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface FlowbasedComputationResult {
    enum Status {
        FAILURE,
        SUCCESS
    }

    Status getStatus();

    DataDomain getFlowBasedDomain();
}
