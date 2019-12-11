/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class MinimumMargin {

    private MinimumMargin() {

    }
/*
    public static double evaluate(Network network, Crac crac) {
        double minimumMargin = Double.MAX_VALUE;
        for (Cnec cnec : crac.getCnecs()) {
            double currentMargin = cnec.computeMargin(network);
            if (currentMargin < minimumMargin) {
                minimumMargin = currentMargin;
            }
        }
        return minimumMargin;
    }
    */
}
