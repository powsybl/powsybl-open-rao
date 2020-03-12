package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractNetworkAction<I extends AbstractNetworkAction<I>> extends AbstractRemedialAction<I> implements NetworkAction<I> {
    public AbstractNetworkAction(String id, String name, String operator, List<UsageRule> usageRules) {
        super(id, name, operator, usageRules);
    }

    public AbstractNetworkAction(String id, String name, String operator) {
        super(id, name, operator);
    }

    public AbstractNetworkAction(String id, String operator) {
        super(id, operator);
    }

    public AbstractNetworkAction(String id) {
        super(id);
    }
}
