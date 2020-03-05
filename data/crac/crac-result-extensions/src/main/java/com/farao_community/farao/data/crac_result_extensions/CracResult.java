/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * Extension of {@link Crac} containing data related to an optimization:
 * <li>
 *     <ul>networkSecurityStatus: can be SECURED or UNSECURED</ul>
 *     <ul>cost: the value of the optimisation minimisation criterion.
 *     If it is negative, the networkSecurityStatus is SECURED</ul>
 * </li>
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracResult extends AbstractExtension<Crac> {

    public enum NetworkSecurityStatus {
        SECURED,
        UNSECURED
    }

    private NetworkSecurityStatus networkSecurityStatus;
    private double cost;

    public NetworkSecurityStatus getNetworkSecurityStatus() {
        return networkSecurityStatus;
    }

    public double getCost() {
        return cost;
    }

    public void setNetworkSecurityStatus() {
        this.networkSecurityStatus = cost <= 0 ? NetworkSecurityStatus.SECURED : NetworkSecurityStatus.UNSECURED;
    }

    public void setCost(double cost) {
        this.cost = cost;
        setNetworkSecurityStatus();
    }

    public CracResult(double cost) {
        this.cost = cost;
        setNetworkSecurityStatus();
    }

    public CracResult() {
        this.cost = Double.NaN;
        setNetworkSecurityStatus();
    }

    @Override
    public String getName() {
        return "CracResult";
    }
}
