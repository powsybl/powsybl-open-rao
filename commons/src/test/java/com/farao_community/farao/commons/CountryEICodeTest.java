/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.powsybl.iidm.network.Country;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya@rte-france.com>}
 */
public class CountryEICodeTest {
    @Test
    public void testEicCodeConvergence() {
        assertEquals(Country.AT, new CountryEICode("10YAT-APG------L").getCountry());
        assertEquals(Country.BE, new CountryEICode("10YBE----------2").getCountry());
        assertEquals(Country.CZ, new CountryEICode("10YCZ-CEPS-----N").getCountry());
        assertEquals(Country.DE, new CountryEICode("10YCB-GERMANY--8").getCountry());
        assertEquals(Country.FR, new CountryEICode("10YFR-RTE------C").getCountry());
        assertEquals(Country.HR, new CountryEICode("10YHR-HEP------M").getCountry());
        assertEquals(Country.HU, new CountryEICode("10YHU-MAVIR----U").getCountry());
        assertEquals(Country.NL, new CountryEICode("10YNL----------L").getCountry());
        assertEquals(Country.PL, new CountryEICode("10YPL-AREA-----S").getCountry());
        assertEquals(Country.RO, new CountryEICode("10YRO-TEL------P").getCountry());
        assertEquals(Country.SI, new CountryEICode("10YSI-ELES-----O").getCountry());
        assertEquals(Country.SK, new CountryEICode("10YSK-SEPS-----K").getCountry());
        assertEquals(Country.ES, new CountryEICode("10YES-REE------0").getCountry());
        assertEquals(Country.RS, new CountryEICode("10YCS-SERBIATSOV").getCountry());
        assertEquals(Country.CH, new CountryEICode("10YCH-SWISSGRIDZ").getCountry());
        assertEquals(Country.PT, new CountryEICode("10YPT-REN------W").getCountry());
        assertEquals(Country.BG, new CountryEICode("10YCA-BULGARIA-R").getCountry());
        assertEquals(Country.AL, new CountryEICode("10YAL-KESH-----5").getCountry());
        assertEquals(Country.TR, new CountryEICode("10YTR-TEIAS----W").getCountry());
        assertEquals(Country.UA, new CountryEICode("10Y1001C--00003F").getCountry());
        assertEquals(Country.MK, new CountryEICode("10YMK-MEPSO----8").getCountry());
        assertEquals(Country.BA, new CountryEICode("10YBA-JPCC-----D").getCountry());
        assertEquals(Country.ME, new CountryEICode("10YCS-CG-TSO---S").getCountry());
        assertEquals(Country.GR, new CountryEICode("10YGR-HTSO-----Y").getCountry());
        assertEquals(Country.IT, new CountryEICode("10YIT-GRTN-----B").getCountry());
    }

    @Test
    public void testEicCodeFromCountry() {
        assertEquals("10YAT-APG------L", new CountryEICode(Country.AT).getCode());
        assertEquals("10YBE----------2", new CountryEICode(Country.BE).getCode());
        assertEquals("10YCZ-CEPS-----N", new CountryEICode(Country.CZ).getCode());
        assertEquals("10YCB-GERMANY--8", new CountryEICode(Country.DE).getCode());
        assertEquals("10YFR-RTE------C", new CountryEICode(Country.FR).getCode());
        assertEquals("10YHR-HEP------M", new CountryEICode(Country.HR).getCode());
        assertEquals("10YHU-MAVIR----U", new CountryEICode(Country.HU).getCode());
        assertEquals("10YNL----------L", new CountryEICode(Country.NL).getCode());
        assertEquals("10YPL-AREA-----S", new CountryEICode(Country.PL).getCode());
        assertEquals("10YRO-TEL------P", new CountryEICode(Country.RO).getCode());
        assertEquals("10YSI-ELES-----O", new CountryEICode(Country.SI).getCode());
        assertEquals("10YSK-SEPS-----K", new CountryEICode(Country.SK).getCode());
        assertEquals("10YES-REE------0", new CountryEICode(Country.ES).getCode());
        assertEquals("10YCS-SERBIATSOV", new CountryEICode(Country.RS).getCode());
        assertEquals("10YCH-SWISSGRIDZ", new CountryEICode(Country.CH).getCode());
        assertEquals("10YPT-REN------W", new CountryEICode(Country.PT).getCode());
        assertEquals("10YCA-BULGARIA-R", new CountryEICode(Country.BG).getCode());
        assertEquals("10YAL-KESH-----5", new CountryEICode(Country.AL).getCode());
        assertEquals("10YTR-TEIAS----W", new CountryEICode(Country.TR).getCode());
        assertEquals("10Y1001C--00003F", new CountryEICode(Country.UA).getCode());
        assertEquals("10YMK-MEPSO----8", new CountryEICode(Country.MK).getCode());
        assertEquals("10YBA-JPCC-----D", new CountryEICode(Country.BA).getCode());
        assertEquals("10YCS-CG-TSO---S", new CountryEICode(Country.ME).getCode());
        assertEquals("10YGR-HTSO-----Y", new CountryEICode(Country.GR).getCode());
        assertEquals("10YIT-GRTN-----B", new CountryEICode(Country.IT).getCode());
    }

}
