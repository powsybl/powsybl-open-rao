/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link Crac} containing data related to an optimization:
 * <ul>
 *     <li>networkSecurityStatus: can be SECURED or UNSECURED</li>
 *     <li>cost: the value of the optimisation minimisation criterion, decomposed into
 *     two components </li>
 *     <li>functionalCost: the meaningful part of the cost, for instance the minimum
 *     margin on all Cnecs.</li>
 *     <li>virtualCost: the virtual part of the cost, typically the costs related to
 *     constraint violations. This cost makes no real sense on a functional viewpoint,
 *     but is necessary for modelling purposes.</li>
 * </ul>
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@JsonTypeName("crac-result")
public class CracResult implements Result {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracResult.class);

    public enum NetworkSecurityStatus {
        SECURED,
        UNSECURED
    }

    private NetworkSecurityStatus networkSecurityStatus;
    private double cost;
    private double functionalCost;
    private double virtualCost;

    public NetworkSecurityStatus getNetworkSecurityStatus() {
        return networkSecurityStatus;
    }

    public void setNetworkSecurityStatus(NetworkSecurityStatus status) {
        this.networkSecurityStatus = status;
    }

    public double getCost() {
        return cost;
    }

    public double getFunctionalCost() {
        return functionalCost;
    }

    public void setFunctionalCost(double functionalCost) {
        this.functionalCost = functionalCost;
        this.cost = this.functionalCost + this.virtualCost;
    }

    public double getVirtualCost() {
        return virtualCost;
    }

    public void setVirtualCost(double virtualCost) {
        this.virtualCost = virtualCost;
        this.cost = this.functionalCost + this.virtualCost;
    }

    @JsonCreator
    public CracResult(@JsonProperty("networkSecurityStatus") NetworkSecurityStatus networkSecurityStatus,
                      @JsonProperty("functionalCost") double functionalCost,
                      @JsonProperty("virtualCost") double virtualCost) {
        this.networkSecurityStatus = networkSecurityStatus;
        this.functionalCost = functionalCost;
        this.virtualCost = virtualCost;
        this.cost = functionalCost + virtualCost;
    }

    public CracResult(double functionalCost) {
        this.functionalCost = functionalCost;
        this.virtualCost = 0;
        this.cost = functionalCost;
    }

    public CracResult() {
        this.cost = Double.NaN;
        this.functionalCost = Double.NaN;
        this.virtualCost = 0;
    }
}
