package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public enum MarketRegulationDirectionKind {
    DOWN("down"),
    UP("up"),
    UP_AND_DOWN("upAndDown"),
    STABLE("stable");

    MarketRegulationDirectionKind(String name) {
        this.name = name;
    }

    private final String name;
    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#MarketRegulationDirectionKind.";
    private static final String URL_25 = NcConstants.NC25_NS_NC_URL + "#MarketRegulationDirectionKind.";

    @Override
    public String toString() {
        return URL + this.name;
    }

    /**
     * Matches a SPARQL result value against this kind, for both the NCP 2.4 and the NCP 2.5 vocabularies.
     */
    public boolean matches(String url) {
        return toString().equals(url) || (URL_25 + name).equals(url);
    }
}
