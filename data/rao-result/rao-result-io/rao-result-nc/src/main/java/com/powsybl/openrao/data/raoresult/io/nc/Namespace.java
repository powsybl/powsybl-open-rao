/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum Namespace {
    CIM("cim", "http://iec.ch/TC57/CIM100#"),
    DCAT("dcat", "http://www.w3.org/ns/dcat#"),
    MD("md", "http://iec.ch/TC57/61970-552/ModelDescription/1#"),
    NC("nc","http://entsoe.eu/ns/nc#"),
    RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    private final String keyword;
    private final String uri;

    Namespace(String keyword, String uri) {
        this.keyword = keyword;
        this.uri = uri;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getURI() {
        return uri;
    }
}
