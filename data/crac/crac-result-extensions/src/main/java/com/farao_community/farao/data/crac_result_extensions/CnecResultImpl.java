/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CnecResultImpl extends AbstractExtension<Cnec> implements CnecResult {

    private double flowInMW;
    private double flowInA;

    public CnecResultImpl(double flowInMW, double flowInA) {
        this.flowInMW = flowInMW;
        this.flowInA = flowInA;
    }

    @Override
    public double getFlowInMW() {
        return flowInMW;
    }

    @Override
    public double getFlowInA() {
        return flowInA;
    }

    @Override
    public CnecResult setFlowInMW(double flow) {
        this.flowInMW = flow;
        return this;
    }

    @Override
    public CnecResult setFlowInA(double flow) {
        this.flowInA = flow;
        return this;
    }
}
