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

public enum Status {
    OPEN("OPEN"),
    CLOSE("CLOSE");

    private static final Map<String, Status> ENUM_MAP = new HashMap<>();

    static {
        for (Status status : Status.values()) {
            ENUM_MAP.put(status.getLabel(), status);
        }
    }

    @Getter
    private final String label;

    Status(String label) {
        this.label = label;
    }

    public static Status fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve status from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
