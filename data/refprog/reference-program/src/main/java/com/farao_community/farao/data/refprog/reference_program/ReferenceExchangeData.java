/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceExchangeData {
    private ReferenceProgramArea areaOut;
    private ReferenceProgramArea areaIn;
    private double flow;

    /**
     * @param areaOut origin country
     * @param areaIn destination country
     * @param flow flow exchanged from origin country to destination country in MW
     */
    public ReferenceExchangeData(ReferenceProgramArea areaOut, ReferenceProgramArea areaIn, double flow) {
        this.areaOut = areaOut;
        this.areaIn = areaIn;
        this.flow = flow;
    }

    public ReferenceProgramArea getAreaOut() {
        return areaOut;
    }

    public ReferenceProgramArea getAreaIn() {
        return areaIn;
    }

    public double getFlow() {
        return flow;
    }

    public void setAreaOut(ReferenceProgramArea areaOut) {
        this.areaOut = areaOut;
    }

    public void setAreaIn(ReferenceProgramArea areaIn) {
        this.areaIn = areaIn;
    }

    public void setFlow(double flow) {
        this.flow = flow;
    }

    boolean isAreaOutToAreaInExchange(ReferenceProgramArea areaOut, ReferenceProgramArea areaIn) {
        return this.areaIn != null && this.areaOut != null && this.areaIn.equals(areaIn) && this.areaOut.equals(areaOut);
    }

    public boolean involvesVirtualHub() {
        return (areaIn != null && areaIn.isVirtualHub()) || (areaOut != null || areaOut.isVirtualHub());
    }
}
