package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;

public abstract class AbstractStandardRangeAction<T extends AbstractRangeAction<T>> extends AbstractRangeAction<T> implements HvdcRangeAction, InjectionRangeAction, CounterTradeRangeAction {

    private final List<StandardRange> ranges;
    private final double initialSetpoint;

    AbstractStandardRangeAction(String id, String name, String operator, String groupId, List<UsageRule> usageRules,
                             List<StandardRange> ranges, double initialSetpoint, Integer speed) {
        super(id, name, operator, usageRules, groupId, speed);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
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
}
