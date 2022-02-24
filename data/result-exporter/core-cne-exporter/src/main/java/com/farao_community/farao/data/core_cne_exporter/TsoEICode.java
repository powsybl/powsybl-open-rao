/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import java.util.Arrays;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public enum TsoEICode {
    AL("AL", "10XAL-KESH-----J", "OST_ALBANIA"),
    AT("AT", "10XAT-APG------Z", "APG"),
    BA("BA", "10XBA-JPCCZEKC-K", "NOSBIH"),
    BE("BE", "10X1001A1001A094", "ELIA"),
    CH("CH", "10XCH-SWISSGRIDC", "SWISSGRID"),
    CY("CY", "10X1001A1001A523", "CYPRUS_TSO"),
    CZ("CZ", "10XCZ-CEPS-GRIDE", "CEPS"),
    D2("D2", "10XDE-EON-NETZ-C", "TTG_DE"),
    D4("D4", "10XDE-ENBW--TNGX", "DE-TRANSNETBWTSO"),
    D7("D7", "10XDE-RWENET---W", "DE-AMPRION-TSO"),
    D8("D8", "10XDE-VE-TRANSMK", "50HERTZ_DE_TSO"),
    DK("DK", "10X1001A1001A248", "ENERGINET-DK"),
    ES("ES", "10XES-REE------E", "REE"),
    FR("FR", "10XFR-RTE------Q", "RTE"),
    FI("FI", "10X1001A1001A264", "FINGRID"),
    GB("GB", "10X1001A1001A515", "NGESO"),
    GR("GR", "10XGR-HTSO-----B", "ADMIE"),
    HR("HR", "10XHR-HEP-OPS--A", "HOPS"),
    HU("HU", "10X1001A1001A329", "MAVIR"),
    IE("IE", "10X1001A1001A531", "EIRGRID"),
    IT("IT", "10X1001A1001A345", "TERNA"),
    LT("LT", "10X1001A1001A55Y", "LITGRID"),
    LV("LV", "10X1001A1001B54W", "LV-AST"),
    ME("ME", "10XCS-CG-TSO---5", "CGES"),
    MK("MK", "10XMK-MEPSO----M", "AD_MEPSO"),
    NL("NL", "10X1001A1001A361", "TENNET_TSO"),
    NO("NO", "10X1001A1001A38Y", "STATNETT_SF"),
    PL("PL", "10XPL-TSO------P", "PSE"),
    PT("PT", "10XPT-REN------9", "REN"),
    RO("RO", "10XRO-TEL------2", "TEL"),
    SE("SE", "10X1001A1001A418", "SVK"),
    SI("SI", "10XSI-ELES-----1", "ELES"),
    SK("SK", "10XSK-SEPS-GRIDB", "SEPS"),
    TR("TR", "10XTR-TEIAS----9", "TEIAS"),
    UA("UA", "10XUA-WPS------K", "UA_WPS"),
    UN("UN", "10XRKS-KOSTT-007", "KOSOVA-TSMO");

    private String shortId;
    private String eiCode;
    private String displayName;

    TsoEICode(String shortId, String eiCode, String displayName) {
        this.shortId = shortId;
        this.eiCode = eiCode;
        this.displayName = displayName;
    }

    public String getShortId() {
        return shortId;
    }

    public String getEICode() {
        return eiCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TsoEICode fromShortId(String shortId) {
        return Arrays.stream(TsoEICode.values()).filter(tsoEICode -> tsoEICode.shortId.equals(shortId)).findAny().orElseThrow();
    }

    public static TsoEICode fromEICode(String eiCode) {
        return Arrays.stream(TsoEICode.values()).filter(tsoEICode -> tsoEICode.eiCode.equals(eiCode)).findAny().orElseThrow();
    }
}

