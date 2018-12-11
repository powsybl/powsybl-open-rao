/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import lombok.Data;

import java.beans.ConstructorProperties;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Data
public class FlowBasedComputationResult {

    public enum Status {
        FAILED,
        SUCCESS
    }

    private final Status status;

    @ConstructorProperties("status")
    public FlowBasedComputationResult(Status status) {
        this.status = status;
    }
}
