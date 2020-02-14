package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PstRange extends RangeAction {

    void setReferenceValue(Network network);

    int computeTapPosition(double finalAngle, PhaseTapChanger phaseTapChanger);
}
