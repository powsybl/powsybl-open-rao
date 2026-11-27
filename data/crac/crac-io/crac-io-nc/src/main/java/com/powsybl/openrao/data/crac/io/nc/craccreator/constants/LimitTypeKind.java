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
public enum LimitTypeKind {
    PATL("patl"),
    TATL("tatl"),
    HIGH_VOLTAGE("highVoltage"),
    LOW_VOLTAGE("lowVoltage");

    LimitTypeKind(String name) {
        this.name = name;
    }

    private final String name;
    private static final String URL = NcConstants.ENTSOE_URL + "LimitTypeKind.";
    private static final String URL_25 = NcConstants.NC25_ENTSOE_URL + "LimitTypeKind.";

    @Override
    public String toString() {
        return URL + this.name;
    }

    /**
     * Matches a SPARQL result value against this kind, for both the NCP 2.4 (CGMES EQ 3.1) and the
     * NCP 2.5 (CGMES EQ 3.2) vocabularies.
     */
    public boolean matches(String url) {
        return toString().equals(url) || (URL_25 + name).equals(url);
    }
}
