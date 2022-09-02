/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.cnec.Side;
import com.powsybl.iidm.network.Branch;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SideTest {
    @Test
    public void basicTests() {
        assertEquals(Branch.Side.ONE, Side.LEFT.iidmSide());
        assertEquals(Branch.Side.TWO, Side.RIGHT.iidmSide());
        assertEquals(Side.LEFT, Side.fromIidmSide(Branch.Side.ONE));
        assertEquals(Side.RIGHT, Side.fromIidmSide(Branch.Side.TWO));
    }
}
