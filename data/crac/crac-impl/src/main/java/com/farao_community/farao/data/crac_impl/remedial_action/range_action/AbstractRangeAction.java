package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction<I extends RangeAction<I>> extends AbstractRemedialAction<I> implements RangeAction<I> {
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
}
