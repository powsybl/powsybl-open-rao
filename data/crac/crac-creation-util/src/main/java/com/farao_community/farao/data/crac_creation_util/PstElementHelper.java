package com.farao_community.farao.data.crac_creation_util;

import java.util.Map;

public interface PstElementHelper extends ElementHelper {

    /**
     * Returns the lowest tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    int getLowTapPosition();

    /**
     * Returns the highest tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    int getHighTapPosition();

    /**
     * Returns the initial tap position of the PST, as defined in the network. Convention is centered on zero.
     */
    int getInitialTap();

    /**
     * Returns the tap to angle conversion map of the PST, as defined in the network. Convention for taps is centered on zero.
     */
    Map<Integer, Double> getTapToAngleConversionMap();
}
