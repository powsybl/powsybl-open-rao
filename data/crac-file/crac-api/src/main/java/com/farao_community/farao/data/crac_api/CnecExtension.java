/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api;

import com.powsybl.commons.extensions.AbstractExtension;

/**
 * Cnec extension for loop flow
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CnecExtension extends AbstractExtension<Cnec> {

    private double loopFlowConstraint; //loop flow constraint used during optimization, set in the search tree
    private double inputLoopFlow; //input loop flow threshold from TSO for each cross zonal Cnec

    public CnecExtension() {
        this.inputLoopFlow = 0.0; // default value 0
    }

    /**
     * set loop flow constraint used during optimization.
     * The value is equal to MAX value of initial loop flow calculated from network and
     * loop flow threshold which is a input parameter from TSO
     * @param loopFlowConstraint = Max(init_Loop_flow, input loop flow)
     */
    void setLoopFlowConstraint(double loopFlowConstraint) {
        this.loopFlowConstraint = loopFlowConstraint;
    }

    /**
     * return loop flow constraint used in linear optimization
     */
    double getLoopFlowConstraint() {
        return loopFlowConstraint;
    }

    /**
     * @return input loop flow threshold parameter from TSO for each cross-zonal Cnec
     */
    double getInputLoopFlow() {
        return inputLoopFlow;
    }

    @Override
    public String getName() {
        return "CnecExtension";
    }
}
