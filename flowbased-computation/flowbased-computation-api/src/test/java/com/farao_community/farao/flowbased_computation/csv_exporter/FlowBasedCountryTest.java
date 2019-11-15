/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.powsybl.iidm.network.Country;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowBasedCountryTest {

    @Test
    public void createFlowBasedCountry() {
        FlowBasedCountry be = new FlowBasedCountry("10YBE----------2");
        FlowBasedCountry ro = new FlowBasedCountry("10YRO-TEL------P");
        FlowBasedCountry frWithTimeStamp = new FlowBasedCountry("10YFR-RTE------C:2019-05-19T23:00:00Z/2019-05-20T23:00:00Z");
        FlowBasedCountry czWithTimeStamp = new FlowBasedCountry("10YCZ-CEPS-----N:2019-05-19T23:00:00Z/2019-05-20T23:00:00Z");
        FlowBasedCountry unknown1 = new FlowBasedCountry("10YXX-UNKNOWN--X");
        FlowBasedCountry unknown2 = new FlowBasedCountry("-");

        assertEquals(be.getName(), Country.BE.getName());
        assertEquals(ro.getName(), Country.RO.getName());
        assertEquals(frWithTimeStamp.getName(), Country.FR.getName());
        assertEquals(czWithTimeStamp.getName(), Country.CZ.getName());
        assertEquals(unknown1.getName(), "10YXX-UNKNOWN--X");
        assertEquals(unknown2.getName(), "-");
    }
}
