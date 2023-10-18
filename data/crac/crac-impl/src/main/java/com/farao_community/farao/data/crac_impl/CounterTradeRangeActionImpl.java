package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com}
 */
public class CounterTradeRangeActionImpl extends AbstractStandardRangeAction<CounterTradeRangeAction> implements CounterTradeRangeAction {

    private final Country exportingCountry;

    CounterTradeRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                             List<StandardRange> ranges, double initialSetpoint, Integer speed, Country exportingCountry) {
        super(id, name, operator, usageRules, groupId, speed, ranges, initialSetpoint);
        this.exportingCountry = exportingCountry;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return null;
    }

    @Override
    public Set<Optional<Country>> getLocation(Network network) {
        return super.getLocation(network);
    }

    @Override
    public Country getExportingCountry() {
        return exportingCountry;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new FaraoException("Can't apply a counter trade range action on a network");
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

        return this.exportingCountry.equals(((CounterTradeRangeAction) o).getExportingCountry());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode += 31 * exportingCountry.hashCode();
        return hashCode;
    }
}
