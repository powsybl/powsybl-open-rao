/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc.profiles;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum NetworkCodeProfile {
    REMEDIAL_ACTION_SCHEDULE("RAS", "https://ap-voc.cim4.eu/RemedialActionSchedule/2.4", "urn:uuid:6e90c546-3c6c-471b-8040-e05037081c59");

    private final String keyword;
    private final String versionIri;
    private final String identifier;

    NetworkCodeProfile(String keyword, String versionIri, String identifier) {
        this.keyword = keyword;
        this.versionIri = versionIri;
        this.identifier = identifier;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getVersionIri() {
        return versionIri;
    }

    public String getIdentifier() {
        return identifier;
    }
}
