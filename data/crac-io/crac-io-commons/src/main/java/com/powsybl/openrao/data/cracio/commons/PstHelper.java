/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons;

import java.util.Map;

public interface PstHelper extends ElementHelper {

    enum TapConvention {
        CENTERED_ON_ZERO, // Taps from -x to x
        STARTS_AT_ONE // Taps from 1 to y
    }

    /**
     * If the PST element is valid, returns a boolean indicating whether or not the element is
     * inverted in the network, compared to the orientation originally used in the constructor
     * of the helper
     */
    boolean isInvertedInNetwork();

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

    /**
     * Converts a tap position of the PST to the used convention (centered on zero).
     * Has no effect if the original convetion is already centered on zero.
     * @param originalTap the original tap position
     * @param originalTapConvention the convention used for the original tap position
     * @return the normalized (centered on zero) tap position
     */
    default int normalizeTap(int originalTap, TapConvention originalTapConvention) {
        if (originalTapConvention.equals(TapConvention.CENTERED_ON_ZERO)) {
            return originalTap;
        } else {
            return getLowTapPosition() + originalTap - 1;
        }
    }
}
