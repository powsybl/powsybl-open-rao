package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction extends AbstractRemedialAction<RangeAction> implements RangeAction {
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
