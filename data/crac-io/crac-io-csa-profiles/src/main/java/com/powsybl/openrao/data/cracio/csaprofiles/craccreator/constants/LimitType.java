/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum LimitType {
    ANGLE(CsaProfileConstants.VOLTAGE_ANGLE_LIMIT),
    CURRENT(CsaProfileConstants.CURRENT_LIMIT),
    VOLTAGE(CsaProfileConstants.VOLTAGE_LIMIT);

    LimitType(String type) {
        this.type = type;
    }

    private final String type;

    @Override
    public String toString() {
        return this.type;
    }
}
