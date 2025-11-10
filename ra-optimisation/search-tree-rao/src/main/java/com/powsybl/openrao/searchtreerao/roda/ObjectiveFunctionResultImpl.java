/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final double functionalCost;
    private final Map<String, Double> virtualCosts;
    private final List<FlowCnec> mostLimitingElements;
    private final List<FlowCnec> costlyElements;

    public ObjectiveFunctionResultImpl(double functionalCost, Map<String, Double> virtualCosts, List<FlowCnec> mostLimitingElements, List<FlowCnec> costlyElements) {
        this.functionalCost = functionalCost;
        this.virtualCosts = virtualCosts;
        this.mostLimitingElements = mostLimitingElements;
        this.costlyElements = costlyElements;
    }

    @Override
    public double getFunctionalCost() {
        return functionalCost;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return mostLimitingElements.subList(0, number);
    }

    @Override
    public double getVirtualCost() {
        return virtualCosts.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return virtualCosts.keySet();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return virtualCosts.get(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return costlyElements.subList(0, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        throw new OpenRaoException("excludeContingencies is not implemented in ObjectiveFunctionResultImpl");
    }

    @Override
    public void excludeCnecs(Set<String> cnecsToExclude) {
        throw new OpenRaoException("excludeCnecs is not implemented in ObjectiveFunctionResultImpl");
    }
}
