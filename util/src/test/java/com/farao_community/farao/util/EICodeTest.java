/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

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

    @Test
    public void testEicCodeFromCountry() {
        assertEquals("10YAT-APG------L", new EICode(Country.AT).getCode());
        assertEquals("10YBE----------2", new EICode(Country.BE).getCode());
        assertEquals("10YCZ-CEPS-----N", new EICode(Country.CZ).getCode());
        assertEquals("10YCB-GERMANY--8", new EICode(Country.DE).getCode());
        assertEquals("10YFR-RTE------C", new EICode(Country.FR).getCode());
        assertEquals("10YHR-HEP------M", new EICode(Country.HR).getCode());
        assertEquals("10YHU-MAVIR----U", new EICode(Country.HU).getCode());
        assertEquals("10YNL----------L", new EICode(Country.NL).getCode());
        assertEquals("10YPL-AREA-----S", new EICode(Country.PL).getCode());
        assertEquals("10YRO-TEL------P", new EICode(Country.RO).getCode());
        assertEquals("10YSI-ELES-----O", new EICode(Country.SI).getCode());
        assertEquals("10YSK-SEPS-----K", new EICode(Country.SK).getCode());
        assertEquals("10YES-REE------0", new EICode(Country.ES).getCode());
        assertEquals("10YCS-SERBIATSOV", new EICode(Country.RS).getCode());
        assertEquals("10YCH-SWISSGRIDZ", new EICode(Country.CH).getCode());
        assertEquals("10YPT-REN------W", new EICode(Country.PT).getCode());
        assertEquals("10YCA-BULGARIA-R", new EICode(Country.BG).getCode());
        assertEquals("10YAL-KESH-----5", new EICode(Country.AL).getCode());
        assertEquals("10YTR-TEIAS----W", new EICode(Country.TR).getCode());
        assertEquals("10Y1001C--00003F", new EICode(Country.UA).getCode());
        assertEquals("10YMK-MEPSO----8", new EICode(Country.MK).getCode());
        assertEquals("10YBA-JPCC-----D", new EICode(Country.BA).getCode());
        assertEquals("10YCS-CG-TSO---S", new EICode(Country.ME).getCode());
        assertEquals("10YGR-HTSO-----Y", new EICode(Country.GR).getCode());
        assertEquals("10YIT-GRTN-----B", new EICode(Country.IT).getCode());
    }

}
