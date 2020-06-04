/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.iidm.network.Country;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * EIC = Energy Identification Code
 */
public class EICode {
    //EIC = Energy Identification Code
    //EICode = Energy Identification Code

    /**
     * number of character in an EI Code
     */
    public static final int LENGTH = 16;

    /**
     * code string
     */
    private String codeString; //find in Market_Areas_v1.0.pdf

    /**
     * @param codeString default constructor
     */
    public EICode(String codeString) {
        this.codeString = codeString;
    }

    /**
     * @return return Country
     */
    public Country getCountry() {
        switch (codeString) {
            case "10YBE----------2" : return Country.BE;
            case "10YSK-SEPS-----K" : return Country.SK;
            case "10YCB-GERMANY--8" : return Country.DE;
            case "10YHU-MAVIR----U" : return Country.HU;
            case "10YNL----------L" : return Country.NL;
            case "10YAT-APG------L" : return Country.AT;
            case "10YCZ-CEPS-----N" : return Country.CZ;
            case "10YHR-HEP------M" : return Country.HR;
            case "10YPL-AREA-----S" : return Country.PL;
            case "10YRO-TEL------P" : return Country.RO;
            case "10YSI-ELES-----O" : return Country.SI;
            case "10YFR-RTE------C" : return Country.FR;
//            case "10YDE-ENBW-----N" : return Country.DE;//a ControlArea
//            case "10YDE-EON------1" : return Country.DE;//b ControlArea
//            case "10YDE-RWENET---I" : return Country.DE;//c ControlArea
//            case "10YDE-VE-------2" : return Country.DE;//d ControlArea
            case "10YES-REE------0" : return Country.ES;
            case "10YCS-SERBIATSOV" : return Country.RS;
            case "10YCH-SWISSGRIDZ" : return Country.CH;
            case "10YPT-REN------W" : return Country.PT;
            case "10YCA-BULGARIA-R" : return Country.BG;
            case "10YAL-KESH-----5" : return Country.AL;
            case "10YTR-TEIAS----W" : return Country.TR;
            case "10Y1001C--00003F" : return Country.UA;
            case "10YMK-MEPSO----8" : return Country.MK;
            case "10YBA-JPCC-----D" : return Country.BA;
            case "10YCS-CG-TSO---S" : return Country.ME;
            case "10YGR-HTSO-----Y" : return Country.GR;
            case "10YIT-GRTN-----B" : return Country.IT;
            default: throw new IllegalArgumentException("Unknown EICode.");
        }
    }

}
