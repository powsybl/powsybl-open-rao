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
public enum OperationalLimitDirectionKind {
    ABSOLUTE("absoluteValue"),
    HIGH("high"),
    LOW("low");

    OperationalLimitDirectionKind(String direction) {
        this.direction = direction;
    }

    private final String direction;
    private static final String URL = CsaProfileConstants.IEC_URL + "CIM100#OperationalLimitDirectionKind.";

    @Override
    public String toString() {
        return URL + this.direction;
    }
}
