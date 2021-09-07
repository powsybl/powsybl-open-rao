/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation_util;

import java.util.Map;

public interface PstHelper extends ElementHelper {

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
