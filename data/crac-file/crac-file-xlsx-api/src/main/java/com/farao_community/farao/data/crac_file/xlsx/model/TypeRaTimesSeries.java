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

public enum TypeRaTimesSeries {
    P0("P0"),
    RDP_HIGH("RDP+"),
    RDP_LOW("RDP-"),
    PMAX("Pmax"),
    PMIN("Pmin"),
    P0_ADAPTATION("P0adaption");

    private static final Map<String, TypeRaTimesSeries> ENUM_MAP = new HashMap<>();

    static {
        for (TypeRaTimesSeries typeRaTimesSeries : TypeRaTimesSeries.values()) {
            ENUM_MAP.put(typeRaTimesSeries.getLabel(), typeRaTimesSeries);
        }
    }

    @Getter
    private final String label;

    TypeRaTimesSeries(String label) {
        this.label = label;
    }

    public static TypeRaTimesSeries fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve type of RA timeseries from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }

}
