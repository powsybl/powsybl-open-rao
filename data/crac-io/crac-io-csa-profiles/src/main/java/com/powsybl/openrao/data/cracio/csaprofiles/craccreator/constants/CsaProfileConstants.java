/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    private CsaProfileConstants() {
    }

    public static final String EXTENSION_FILE_CSA_PROFILE = "zip";
    public static final String RDF_BASE_URL = "http://entsoe.eu";
    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";
    public static final String SPARQL_FILE_CSA_PROFILE = "csa_profile.sparql";

    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String KEYWORD = "keyword";

    // TODO: replace by classes

    public static final String CURRENT_LIMIT = "currentLimit";
    public static final String VOLTAGE_LIMIT = "voltageLimit";
    public static final String VOLTAGE_ANGLE_LIMIT = "voltageAngleLimit";
}
