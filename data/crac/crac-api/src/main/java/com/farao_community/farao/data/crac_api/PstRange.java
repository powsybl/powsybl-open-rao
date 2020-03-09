package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PstRange extends RangeAction<PstRange> {

    int computeTapPosition(double finalAngle);

    int getCurrentTapPosition(Network network, RangeDefinition rangeDefinition);
}
