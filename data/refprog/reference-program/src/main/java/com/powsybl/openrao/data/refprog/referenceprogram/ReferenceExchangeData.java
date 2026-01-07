/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.openrao.commons.EICode;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceExchangeData {
    private EICode areaOut;
    private EICode areaIn;
    private double flow;

    /**
     * @param areaOut origin country
     * @param areaIn destination country
     * @param flow flow exchanged from origin country to destination country in MW
     */
    public ReferenceExchangeData(EICode areaOut, EICode areaIn, double flow) {
        this.areaOut = areaOut;
        this.areaIn = areaIn;
        this.flow = flow;
    }

    public EICode getAreaOut() {
        return areaOut;
    }

    public EICode getAreaIn() {
        return areaIn;
    }

    public double getFlow() {
        return flow;
    }

    public void setAreaOut(EICode areaOut) {
        this.areaOut = areaOut;
    }

    public void setAreaIn(EICode areaIn) {
        this.areaIn = areaIn;
    }

    public void setFlow(double flow) {
        this.flow = flow;
    }

    boolean isAreaOutToAreaInExchange(EICode areaOut, EICode areaIn) {
        return this.areaIn != null && this.areaOut != null && this.areaIn.equals(areaIn) && this.areaOut.equals(areaOut);
    }
}
