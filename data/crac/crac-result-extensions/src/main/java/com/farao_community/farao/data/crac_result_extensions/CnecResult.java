/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@JsonTypeName("cnec-result")
public class CnecResult implements Result<Cnec> {

    private double flowInMW;
    private double flowInA;

    @JsonCreator
    public CnecResult(@JsonProperty("flowInMw") double flowInMW, @JsonProperty("flowInA") double flowInA) {
        this.flowInMW = flowInMW;
        this.flowInA = flowInA;
    }

    public CnecResult(double flowInMW) {
        this.flowInMW = flowInMW;
        this.flowInA = Double.NaN;
    }

    public CnecResult() {
        this.flowInMW = Double.NaN;
        this.flowInA = Double.NaN;
    }

    public void setFlowInMW(double flow) {
        this.flowInMW = flow;
    }

    public double getFlowInMW() {
        return flowInMW;
    }

    public void setFlowInA(double flow) {
        this.flowInA = flow;
    }

    public double getFlowInA() {
        return flowInA;
    }
}
