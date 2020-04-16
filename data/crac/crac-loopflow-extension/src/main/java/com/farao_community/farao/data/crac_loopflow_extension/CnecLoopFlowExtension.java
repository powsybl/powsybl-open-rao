/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * Cnec extension for loop flow
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CnecLoopFlowExtension extends AbstractExtension<Cnec> {

    private double inputLoopFlow; //input loop flow threshold from TSO for each cross zonal Cnec. absolute value in MW

    public CnecLoopFlowExtension() {
        this.inputLoopFlow = 0.0; // default value 0
    }

    public CnecLoopFlowExtension(double inputLoopFlow) {
        this.inputLoopFlow = inputLoopFlow;
    }

    /**
     * @return input loop flow threshold parameter from TSO for each cross-zonal Cnec
     */
    public double getInputLoopFlow() {
        return inputLoopFlow;
    }

    @Override
    public String getName() {
        return "CnecLoopFlowExtension";
    }
}
