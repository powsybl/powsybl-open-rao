package com.farao_community.farao.data.crac_api;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PstRange extends RangeAction<PstRange> {

    int computeTapPosition(double finalAngle);
}
