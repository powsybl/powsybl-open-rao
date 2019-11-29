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

public enum ElementDescriptionMode {
    ELEMENT_NAME("ElementName"),
    ORDER_CODE("OrderCode");

    private static final Map<String, ElementDescriptionMode> ENUM_MAP = new HashMap<>();

    static {
        for (ElementDescriptionMode elementDescriptionMode : ElementDescriptionMode.values()) {
            ENUM_MAP.put(elementDescriptionMode.getLabel(), elementDescriptionMode);
        }
    }

    @Getter
    private final String label;

    ElementDescriptionMode(String label) {
        this.label = label;
    }

    public static ElementDescriptionMode fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve type of element description mode from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
