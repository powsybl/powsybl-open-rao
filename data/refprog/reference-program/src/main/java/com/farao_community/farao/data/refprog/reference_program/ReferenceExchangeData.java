/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.network.Country;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceExchangeData {
    private Country areaOut;
    private Country areaIn;
    private double flow;

    /**
     * @param areaOut origin country
     * @param areaIn destination country
     * @param flow flow exchanged from origin country to destination country in MW
     */
    public ReferenceExchangeData(Country areaOut, Country areaIn, double flow) {
        this.areaOut = areaOut;
        this.areaIn = areaIn;
        this.flow = flow;
    }

    public Country getAreaOut() {
        return areaOut;
    }

    public Country getAreaIn() {
        return areaIn;
    }

    public double getFlow() {
        return flow;
    }

    public void setAreaOut(Country areaOut) {
        this.areaOut = areaOut;
    }

    public void setAreaIn(Country areaIn) {
        this.areaIn = areaIn;
    }

    public void setFlow(double flow) {
        this.flow = flow;
    }

    boolean isAreaOutToAreaInExchange(Country areaOutId, Country areaInId) {
        return this.areaIn != null && this.areaOut != null && this.areaIn.equals(areaInId) && this.areaOut.equals(areaOutId);
    }
}
