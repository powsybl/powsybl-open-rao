/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.powsybl.iidm.network.Country;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class EICodeTest {
    @Test
    public void testEicCodeConvergence() {
        assertEquals(Country.AT, new EICode("10YAT-APG------L").getCountry());
        assertEquals(Country.BE, new EICode("10YBE----------2").getCountry());
        assertEquals(Country.CZ, new EICode("10YCZ-CEPS-----N").getCountry());
        assertEquals(Country.DE, new EICode("10YCB-GERMANY--8").getCountry());
        assertEquals(Country.FR, new EICode("10YFR-RTE------C").getCountry());
        assertEquals(Country.HR, new EICode("10YHR-HEP------M").getCountry());
        assertEquals(Country.HU, new EICode("10YHU-MAVIR----U").getCountry());
        assertEquals(Country.NL, new EICode("10YNL----------L").getCountry());
        assertEquals(Country.PL, new EICode("10YPL-AREA-----S").getCountry());
        assertEquals(Country.RO, new EICode("10YRO-TEL------P").getCountry());
        assertEquals(Country.SI, new EICode("10YSI-ELES-----O").getCountry());
        assertEquals(Country.SK, new EICode("10YSK-SEPS-----K").getCountry());
    }
}
