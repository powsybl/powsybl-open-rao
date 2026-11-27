/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum RemedialActionDependencyKind {
    BALANCED("balanced"),
    EXCLUSIVE("exclusive"),
    INCLUSIVE("inclusive"),
    NONE("none"),
    RESTRICTIVE("restrictive");

    private final String name;
    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#RemedialActionDependencyKind.";
    private static final String URL_25 = NcConstants.NC25_NS_NC_URL + "#RemedialActionDependencyKind.";

    RemedialActionDependencyKind(String name) {
        this.name = name;
    }

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
