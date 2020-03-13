package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction extends AbstractRemedialAction implements RangeAction {
    protected List<Range> ranges;

    public AbstractRangeAction(String id, String name, String operator, List<UsageRule> usageRules, List<Range> ranges) {
        super(id, name, operator, usageRules);
        this.ranges = new ArrayList<>(ranges);
    }

    public AbstractRangeAction(String id, String name, String operator) {
        super(id, name, operator);
        this.ranges = new ArrayList<>();
    }

    public AbstractRangeAction(String id, String operator) {
        super(id, operator);
        this.ranges = new ArrayList<>();
    }

    public AbstractRangeAction(String id) {
        super(id);
        this.ranges = new ArrayList<>();
    }

    public final List<Range> getRanges() {
        return ranges;
    }

    public void addRange(Range range) {
        this.ranges.add(range);
    }

    protected double getMinValueWithRange(Network network, Range range) {
        return range.getMin();
    }

    protected double getMaxValueWithRange(Network network, Range range) {
        return range.getMax();
    }

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.NEGATIVE_INFINITY;
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.POSITIVE_INFINITY;
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    @Override
    public void synchronize(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void desynchronize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRangeAction otherAbstractRangeAction = (AbstractRangeAction) o;

        return super.equals(o)
            && new HashSet<>(ranges).equals(new HashSet<>(otherAbstractRangeAction.ranges));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (Range range : ranges) {
            result = 31 * result + range.hashCode();
        }
        return result;
    }
}
