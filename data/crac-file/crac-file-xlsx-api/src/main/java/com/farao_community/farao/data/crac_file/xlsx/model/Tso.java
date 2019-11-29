/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.commons.FaraoException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum Tso {
    P_50HERTZ("p50Hertz"),
    P_APG("pAPG"),
    P_CEPS("pCEPS"),
    P_ELES("pELES"),
    P_MAVIR("pMAVIR"),
    P_PSE("pPSE"),
    P_TTG("pTTG"),
    P_TTB("pTTB"),
    P_AMPRION("pAMPRION"),
    P_SWG("pSWG"),
    P_HOPS("pHOPS"),
    P_TNG("pTNG"),
    P_ENERGINET("pENERGINET"),
    P_CREOS("pCREOS"),
    N_ELIA("nELIA"),
    N_RTE("nRTE"),
    N_TERNA("nTERNA"),
    N_NOSBIH("nNOSBIH"),
    N_EMS("nEMS"),
    N_TEL("nTEL"),
    N_SEPS("nSEPS"),
    N_ISOBIH("nISOBIH"),
    N_KOSTT("nKOSTT"),
    N_UA("nUA"),
    N_AD("nAD"),
    N_AL("nAL"),
    N_ESO("nESO"),
    N_HTSO("nHTSO"),
    N_MEPSO("nMEPSO"),
    N_REE("nREE"),
    N_REN("nREN"),
    N_TEIAS("nTEIAS"),
    N_TRANSELECTRICA("nTRANSELECTRICA");

    private static final Map<String, Tso> ENUM_MAP = new HashMap<>();

    static {
        for (Tso tso : Tso.values()) {
            ENUM_MAP.put(tso.getLabel(), tso);
        }
    }

    @Getter
    private final String label;

    Tso(String label) {
        this.label = label;
    }

    public static Tso fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve type of tso from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
