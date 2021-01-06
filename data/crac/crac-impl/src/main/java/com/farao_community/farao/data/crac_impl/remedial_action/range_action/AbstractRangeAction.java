package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction extends AbstractRemedialAction<RangeAction> implements RangeAction {
    protected List<Range> ranges = new ArrayList<>();
    protected NetworkElement networkElement;
    protected String groupId = null;

    public AbstractRangeAction(String id, String name, String operator, List<UsageRule> usageRules,
                                         List<Range> ranges, NetworkElement networkElement, String groupId) {
        super(id, name, operator, usageRules);
        this.ranges = new ArrayList<>(ranges);
        this.networkElement = networkElement;
        this.groupId = groupId;
    }

    public AbstractRangeAction(String id, String name, String operator, List<UsageRule> usageRules,
                                         List<Range> ranges, NetworkElement networkElement) {
        this(id, name, operator, usageRules, ranges, networkElement, null);
    }

    public AbstractRangeAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.networkElement = networkElement;
    }

    public AbstractRangeAction(String id, NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
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

    protected abstract double getMinValueWithRange(Network network, Range range);

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.NEGATIVE_INFINITY;
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    protected abstract double getMaxValueWithRange(Network network, Range range);

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.POSITIVE_INFINITY;
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public Optional<String> getGroupId() {
        return Optional.ofNullable(groupId);
    }

    public final List<Range> getRanges() {
        return ranges;
    }

    public void addRange(Range range) {
        this.ranges.add(range);
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
                && new HashSet<>(ranges).equals(new HashSet<>(otherAbstractRangeAction.ranges))
                && networkElement.equals(otherAbstractRangeAction.getNetworkElement());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (Range range : ranges) {
            result = 31 * result + range.hashCode();
        }
        result = 31 * result + networkElement.hashCode();
        return result;
    }
}
