/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Branch;

/**
 * Side of a branch
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum Side {
    LEFT(Branch.Side.ONE),
    RIGHT(Branch.Side.TWO);

    private final Branch.Side iidmSide;

    Side(Branch.Side iidmSide) {
        this.iidmSide = iidmSide;
    }

    public static Side fromIidmSide(Branch.Side side) {
        return switch (side) {
            case ONE -> LEFT;
            case TWO -> RIGHT;
            default -> throw new FaraoException(String.format("Unhandled iidm side: %s", side));
        };
    }

    public Branch.Side iidmSide() {
        return iidmSide;
    }
}
