/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class AbstractSetpointElementaryNetworkActionTest {
    @Test
    public void testHashCode() {
        AbstractSetpointElementaryNetworkAction hvdcSetpoint = new HvdcSetpoint("hvdcId", new NetworkElement("neId"), 10);
        AbstractSetpointElementaryNetworkAction hvdcSetpointEqual = new HvdcSetpoint("hvdcId", new NetworkElement("neId"), 10);
        AbstractSetpointElementaryNetworkAction hvdcSetpointDifferentById = new HvdcSetpoint("hvdcId2", new NetworkElement("neId"), 10);
        AbstractSetpointElementaryNetworkAction hvdcSetpointDifferentByNE = new HvdcSetpoint("hvdcId", new NetworkElement("neId2"), 10);
        AbstractSetpointElementaryNetworkAction hvdcSetpointDifferentBySetpoint = new HvdcSetpoint("hvdcId", new NetworkElement("neId"), 15);

        assertEquals(hvdcSetpoint, hvdcSetpointEqual);
        assertNotEquals(hvdcSetpoint, hvdcSetpointDifferentById);
        assertNotEquals(hvdcSetpoint, hvdcSetpointDifferentByNE);
        assertNotEquals(hvdcSetpoint, hvdcSetpointDifferentBySetpoint);

        assertEquals(hvdcSetpoint.hashCode(), hvdcSetpointEqual.hashCode());
        assertNotEquals(hvdcSetpoint.hashCode(), hvdcSetpointDifferentById.hashCode());
        assertNotEquals(hvdcSetpoint.hashCode(), hvdcSetpointDifferentByNE.hashCode());
        assertNotEquals(hvdcSetpoint.hashCode(), hvdcSetpointDifferentBySetpoint.hashCode());

        assertNotEquals(hvdcSetpoint, null);
    }
}
