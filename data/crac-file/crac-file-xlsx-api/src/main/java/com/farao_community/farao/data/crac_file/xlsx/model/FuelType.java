/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.commons.FaraoException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public enum FuelType {
    COAL("Coal"),
    HYDRO("Hydro(NonPS)"),
    NUCLEAR("Nuclear"),
    OIL("Oil"),
    PUMP_STORAGE("PumpStorage"),
    PV("PV"),
    WIND("Wind"),
    OTHER("Other");

    private static final Map<String, FuelType> ENUM_MAP = new HashMap<>();

    static {
        for (FuelType fuelType : FuelType.values()) {
            ENUM_MAP.put(fuelType.getLabel(), fuelType);
        }
    }

    @Getter
    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    public static FuelType fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve fuel type from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
