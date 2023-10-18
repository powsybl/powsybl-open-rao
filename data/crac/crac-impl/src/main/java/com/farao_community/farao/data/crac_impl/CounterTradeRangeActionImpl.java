package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

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

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        double minAdmissibleSetpoint = Double.NEGATIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, range.getMin());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, initialSetpoint + range.getMin());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, previousInstantSetPoint + range.getMin());
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return minAdmissibleSetpoint;
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        double maxAdmissibleSetpoint = Double.POSITIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, range.getMax());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, initialSetpoint + range.getMax());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, previousInstantSetPoint + range.getMax());
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return maxAdmissibleSetpoint;
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

        return this.exportingCountry.equals(((CounterTradeRangeAction) o).getExportingCountry())
                && this.importingCountry.equals(((CounterTradeRangeAction) o).getImportingCountry());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode += 31 * exportingCountry.hashCode();
        return hashCode;
    }
}
