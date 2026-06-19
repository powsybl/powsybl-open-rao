/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionImpl extends AbstractRangeAction<CounterTradeRangeAction> implements CounterTradeRangeAction {

    private final String exportingArea;
    private final String importingArea;
    private final List<StandardRange> ranges;
    private final Double initialSetpoint;

    CounterTradeRangeActionImpl(String id,
                                String name,
                                String operator,
                                String groupId,
                                Set<UsageRule> usageRules,
                                List<StandardRange> ranges,
                                Double initialSetpoint,
                                Integer speed,
                                Double activationCost,
                                Map<VariationDirection, Double> variationCosts,
                                String exportingArea,
                                String importingArea) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.exportingArea = exportingArea;
        this.importingArea = importingArea;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMinAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMaxAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public Double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.emptySet();
    }

    @Override
    public String getExportingArea() {
        return exportingArea;
    }

    @Override
    public String getImportingArea() {
        return importingArea;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new OpenRaoException("Can't apply a counter trade range action on a network");
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        return this.exportingArea.equals(((CounterTradeRangeAction) o).getExportingArea())
                && this.importingArea.equals(((CounterTradeRangeAction) o).getImportingArea())
                && this.ranges.equals(((CounterTradeRangeAction) o).getRanges());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * exportingArea.hashCode() + 63 * importingArea.hashCode();
        return hashCode;
    }
}
