/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CostResult {

    private double functionalCost;
    private final Map<String, Double> virtualCosts;

    CostResult() {
        functionalCost = Double.NaN;
        virtualCosts = new HashMap<>();
    }

    public double getCost() {
        double virtualCost = getVirtualCost();

        if (Double.isNaN(functionalCost) && Double.isNaN(virtualCost)) {
            return Double.NaN;
        }

        return (Double.isNaN(functionalCost) ? 0 : functionalCost) + (Double.isNaN(virtualCost) ? 0 : virtualCost);
    }

    public double getFunctionalCost() {
        return functionalCost;
    }

    public double getVirtualCost() {
        return virtualCosts.isEmpty() ? Double.NaN : virtualCosts.values().stream().mapToDouble(cost -> cost).filter(cost -> !Double.isNaN(cost)).sum();
    }

    public double getVirtualCost(String virtualCostName) {
        return virtualCosts.getOrDefault(virtualCostName, Double.NaN);
    }

    public Set<String> getVirtualCostNames() {
        return virtualCosts.keySet();
    }

    public void setFunctionalCost(double functionalCost) {
        this.functionalCost = functionalCost;
    }

    public void setVirtualCost(String virtualCostName, double virtualCost) {
        virtualCosts.put(virtualCostName, virtualCost);
    }
}
