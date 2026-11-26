/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

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
    private static final String URL = NcConstants.IEC_URL + "CIM100#OperationalLimitDirectionKind.";
    private static final String URL_25 = NcConstants.NC25_CIM_URL + "#OperationalLimitDirectionKind.";

    @Override
    public String toString() {
        return URL + this.direction;
    }

    /**
     * Matches a SPARQL result value against this kind, for both the NCP 2.4 and the NCP 2.5 vocabularies.
     */
    public boolean matches(String url) {
        return toString().equals(url) || (URL_25 + direction).equals(url);
    }
}
