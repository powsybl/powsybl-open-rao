/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_api.cnec;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.iidm.network.TwoSides;

/**
 * Side of a branch
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum Side {
    LEFT(TwoSides.ONE),
    RIGHT(TwoSides.TWO);

    private final TwoSides iidmSide;

    Side(TwoSides iidmSide) {
        this.iidmSide = iidmSide;
    }

    public TwoSides iidmSide() {
        return iidmSide;
    }

    public static Side fromIidmSide(TwoSides side) {
        switch (side) {
            case ONE:
                return LEFT;
            case TWO:
                return RIGHT;
            default:
                throw new FaraoException(String.format("Unhandled iidm side: %s", side));
        }
    }
}
