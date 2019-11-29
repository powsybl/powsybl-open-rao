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

public enum TypeOfTimeSeries {
    IMAX_TATL("Imax_TATL"),
    IMAX_PATL("Imax_PATL");

    private static final Map<String, TypeOfTimeSeries> ENUM_MAP = new HashMap<>();

    static {
        for (TypeOfTimeSeries typeOfTimeSeries : TypeOfTimeSeries.values()) {
            ENUM_MAP.put(typeOfTimeSeries.getLabel(), typeOfTimeSeries);
        }
    }

    @Getter
    private final String label;

    TypeOfTimeSeries(String label) {
        this.label = label;
    }

    public static TypeOfTimeSeries fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve type of timeseries from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
