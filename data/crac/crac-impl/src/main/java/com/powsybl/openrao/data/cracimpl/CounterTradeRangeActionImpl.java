/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.range.StandardRange;
import com.powsybl.openrao.data.cracapi.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com}
 */
public class CounterTradeRangeActionImpl extends AbstractRangeAction<CounterTradeRangeAction> implements CounterTradeRangeAction {

    private final Country exportingCountry;
    private final Country importingCountry;
    private final List<StandardRange> ranges;
    private final double initialSetpoint;

    CounterTradeRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                             List<StandardRange> ranges, double initialSetpoint, Integer speed, Country exportingCountry, Country importingCountry) {
        super(id, name, operator, usageRules, groupId, speed);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.exportingCountry = exportingCountry;
        this.importingCountry = importingCountry;
    }

    CounterTradeRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                                List<StandardRange> ranges, double initialSetpoint, Integer speed, Country exportingCountry, Country importingCountry, double activationCost) {
        super(id, name, operator, usageRules, groupId, speed, activationCost);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.exportingCountry = exportingCountry;
        this.importingCountry = importingCountry;
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
    public double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.emptySet();
    }

    @Override
    public Country getExportingCountry() {
        return exportingCountry;
    }

    @Override
    public Country getImportingCountry() {
        return importingCountry;
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

        return this.exportingCountry.equals(((CounterTradeRangeAction) o).getExportingCountry())
                && this.importingCountry.equals(((CounterTradeRangeAction) o).getImportingCountry())
                && this.ranges.equals(((CounterTradeRangeAction) o).getRanges());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * exportingCountry.hashCode() + 63 * importingCountry.hashCode();
        return hashCode;
    }
}
