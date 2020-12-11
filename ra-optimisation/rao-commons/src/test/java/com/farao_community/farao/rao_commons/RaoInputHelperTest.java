/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AlreadySynchronizedException;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoInputHelperTest {

    private Network network;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    public void testSynchronizeOk() {
        Crac crac = CommonCracCreation.create();
        RaoInputHelper.synchronize(crac, network);
        assertTrue(crac.isSynchronized());
    }

    @Test
    public void testSynchronizeNotFailSecondTime() {
        Crac crac = CommonCracCreation.create();
        RaoInputHelper.synchronize(crac, network);
        try {
            RaoInputHelper.synchronize(crac, network);
        } catch (AlreadySynchronizedException e) {
            fail("AlreadySynchronizedException should not be thrown");
        }
    }
}
