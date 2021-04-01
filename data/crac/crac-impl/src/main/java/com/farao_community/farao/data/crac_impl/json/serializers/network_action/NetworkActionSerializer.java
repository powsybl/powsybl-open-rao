package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.json.serializers.AbstractRemedialActionSerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionSerializer<E extends AbstractRemedialAction<NetworkAction> & NetworkAction> extends AbstractRemedialActionSerializer<NetworkAction, E> {
}
