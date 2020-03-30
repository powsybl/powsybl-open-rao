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
 * @author Amira Kahya {@literal <amira.kahya@rte-france.com>}
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
        assertEquals(Country.ES, new EICode("10YES-REE------0").getCountry());
        assertEquals(Country.RS, new EICode("10YCS-SERBIATSOV").getCountry());
        assertEquals(Country.CH, new EICode("10YCH-SWISSGRIDZ").getCountry());
        assertEquals(Country.PT, new EICode("10YPT-REN------W").getCountry());
        assertEquals(Country.BG, new EICode("10YCA-BULGARIA-R").getCountry());
        assertEquals(Country.AL, new EICode("10YAL-KESH-----5").getCountry());
        assertEquals(Country.TR, new EICode("10YTR-TEIAS----W").getCountry());
        assertEquals(Country.UA, new EICode("10Y1001C--00003F").getCountry());
        assertEquals(Country.MK, new EICode("10YMK-MEPSO----8").getCountry());
        assertEquals(Country.BA, new EICode("10YBA-JPCC-----D").getCountry());
        assertEquals(Country.ME, new EICode("10YCS-CG-TSO---S").getCountry());
        assertEquals(Country.GR, new EICode("10YGR-HTSO-----Y").getCountry());
        assertEquals(Country.IT, new EICode("10YIT-GRTN-----B").getCountry());
    }
}
