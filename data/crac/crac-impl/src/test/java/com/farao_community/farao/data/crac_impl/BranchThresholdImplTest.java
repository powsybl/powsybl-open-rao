/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class BranchThresholdImplTest {

    @Test
    public void testEqualsAndHashcode() {

        BranchThreshold t1 = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE);
        BranchThreshold t1same = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE);

        BranchThreshold t2 = new BranchThresholdImpl(Unit.MEGAWATT, -500., 500., BranchThresholdRule.ON_LEFT_SIDE);
        BranchThreshold t3 = new BranchThresholdImpl(Unit.MEGAWATT, null, 600., BranchThresholdRule.ON_LEFT_SIDE);
        BranchThreshold t4 = new BranchThresholdImpl(Unit.AMPERE, null, 500., BranchThresholdRule.ON_LEFT_SIDE);
        BranchThreshold t5 = new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_RIGHT_SIDE);

        assertEquals(t1, t1same);
        assertEquals(t1.hashCode(), t1same.hashCode());

        assertNotEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);
    }

}
