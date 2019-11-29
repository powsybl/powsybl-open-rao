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

public enum SharingDefinition {
    NSRA("NSRA - Not Shared RA"),
    CSRA_CONTROL_AREA("CSRA - Shared for the CBCOs within the TSO's Control Area"),
    CSRA_CONTROL_BLOCK("CSRA - Shared for the CBCOs within the TSO's Control Block"),
    CSRA_CBCO_GROUP("CSRA - Shared for the CBCOs within the CBCO Group"),
    CSRA_CBCO("CSRA - Shared for the CBCOs within the CBCO Single"),
    SRA("SRA - Shared RA");

    private static final Map<String, SharingDefinition> ENUM_MAP = new HashMap<>();

    static {
        for (SharingDefinition sharingDefinition : SharingDefinition.values()) {
            ENUM_MAP.put(sharingDefinition.getLabel(), sharingDefinition);
        }
    }

    @Getter
    private final String label;

    SharingDefinition(String label) {
        this.label = label;
    }

    public static SharingDefinition fromLabel(String label) {
        if (!ENUM_MAP.containsKey(label)) {
            throw new FaraoException(String.format("Impossible to retrieve sharing definition from label '%s'", label));
        }
        return ENUM_MAP.get(label);
    }
}
