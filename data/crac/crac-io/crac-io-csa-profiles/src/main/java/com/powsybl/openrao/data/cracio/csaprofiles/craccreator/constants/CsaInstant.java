/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

import com.powsybl.openrao.data.cracapi.InstantKind;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum CsaInstant {
    PREVENTIVE("preventive", InstantKind.PREVENTIVE),
    OUTAGE("outage", InstantKind.OUTAGE),
    AUTO("auto", InstantKind.AUTO),
    CURATIVE_1("curative 1", InstantKind.CURATIVE),
    CURATIVE_2("curative 2", InstantKind.CURATIVE),
    CURATIVE_3("curative 3", InstantKind.CURATIVE);

    private final String instantName;
    private final InstantKind instantKind;

    CsaInstant(String instantName, InstantKind instantKind) {
        this.instantName = instantName;
        this.instantKind = instantKind;
    }

    public String getInstantName() {
        return instantName;
    }

    public InstantKind getInstantKind() {
        return instantKind;
    }
}
