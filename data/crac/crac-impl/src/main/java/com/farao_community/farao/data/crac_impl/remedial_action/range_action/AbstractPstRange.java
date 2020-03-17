package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractPstRange extends AbstractElementaryRangeAction<PstRange> implements PstRange {
    public AbstractPstRange(String id, String name, String operator, List<UsageRule> usageRules, List<Range> ranges, NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges, networkElement);
    }

    public AbstractPstRange(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator, networkElement);
    }

    public AbstractPstRange(String id, NetworkElement networkElement) {
        super(id, networkElement);
    }
}
