/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

import java.util.List;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_1_INSTANT = "curative 1";
    public static final String CURATIVE_2_INSTANT = "curative 2";
    public static final String CURATIVE_3_INSTANT = "curative 3";

    private CsaProfileConstants() {
    }

    /**
     * constants to read rdf files
     */

    public static final String EXTENSION_FILE_CSA_PROFILE = "zip";

    public static final String RDF_BASE_URL = "http://entsoe.eu";

    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";

    /**
     * constants to access triplestore data
     */

    public static final String SPARQL_FILE_CSA_PROFILE = "csa_profile.sparql";

    /**
     * constants to request file headers
     */

    public static final String START_DATE = "startDate";

    public static final String END_DATE = "endDate";

    public static final String KEYWORD = "keyword";

    /**
     * requests for contingencies
     */

    public static final String NORMAL_MUST_STUDY = "normalMustStudy";

    public static final String MUST_STUDY = "mustStudy";

    /**
     * remedial actions
     */

    public static final String SIPS = "RemedialActionSchemeKind.sips";
    public static final String NORMAL_ARMED = "normalArmed";
    public static final String ARMED = "armed";
    public static final String NORMAL_AVAILABLE = "normalAvailable";
    public static final String AVAILABLE = "available";
    public static final String NORMAL_ENABLED = "normalEnabled";
    public static final String ENABLED = "enabled";
    public static final String NORMAL_VALUE = "normalValue";

    /**
     * requests for flow cnec
     */

    public static final String IS_COMBINABLE_WITH_CONTINGENCY = "isCombinableWithContingency";
    public static final String IS_COMBINABLE_WITH_REMEDIAL_ACTION = "isCombinableWithRemedialAction";
    public static final String FLOW_RELIABILITY_MARGIN = "flowReliabilityMargin";

    public static final String CURRENT_LIMIT = "currentLimit";
    public static final String VOLTAGE_LIMIT = "voltageLimit";
    public static final String VALUE = "value";
    public static final String IS_INFINITE_DURATION = "isInfiniteDuration";
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary_1");
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT = List.of("CGMES.Terminal2", "CGMES.Terminal_Boundary_2");
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary");

    /**
     * requests for angle cnec
     */

    public static final String VOLTAGE_ANGLE_LIMIT = "voltageAngleLimit";
    public static final String SCENARIO_TIME = "scenarioTime";
}
