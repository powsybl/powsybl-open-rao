/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_api;

import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SideTest {
    @Test
    void basicTests() {
        assertEquals(TwoSides.ONE, Side.LEFT.iidmSide());
        assertEquals(TwoSides.TWO, Side.RIGHT.iidmSide());
        assertEquals(Side.LEFT, Side.fromIidmSide(TwoSides.ONE));
        assertEquals(Side.RIGHT, Side.fromIidmSide(TwoSides.TWO));
    }
}
