/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GlobalRaoResult implements ObjectiveFunctionResult, TemporalData<RaoResult> {
    private final ObjectiveFunctionResult globalObjectiveFunctionResult;
    private final TemporalData<RaoResult> raoResultPerTimestamp;

    public GlobalRaoResult(ObjectiveFunctionResult globalObjectiveFunctionResult, TemporalData<RaoResult> raoResultPerTimestamp) {
        this.globalObjectiveFunctionResult = globalObjectiveFunctionResult;
        this.raoResultPerTimestamp = raoResultPerTimestamp;
    }

    @Override
    public Map<OffsetDateTime, RaoResult> getDataPerTimestamp() {
        return raoResultPerTimestamp.getDataPerTimestamp();
    }

    @Override
    public List<OffsetDateTime> getTimestamps() {
        return raoResultPerTimestamp.getTimestamps();
    }

    @Override
    public void add(OffsetDateTime timestamp, RaoResult data) {
        raoResultPerTimestamp.add(timestamp, data);
    }

    @Override
    public <U> TemporalData<U> map(Function<RaoResult, U> function) {
        return raoResultPerTimestamp.map(function);
    }

    @Override
    public double getFunctionalCost() {
        return globalObjectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return globalObjectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return globalObjectiveFunctionResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return globalObjectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return globalObjectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return globalObjectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        globalObjectiveFunctionResult.excludeContingencies(contingenciesToExclude);
    }
}
