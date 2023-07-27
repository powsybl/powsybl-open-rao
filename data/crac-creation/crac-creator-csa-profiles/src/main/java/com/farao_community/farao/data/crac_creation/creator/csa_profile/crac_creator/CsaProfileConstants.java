/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import java.util.List;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

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

    public static final String REQUEST_HEADER_START_DATE = "startDate";

    public static final String REQUEST_HEADER_END_DATE = "endDate";

    public static final String REQUEST_HEADER_KEYWORD = "keyword";

    /**
     * requests for contingencies
     */

    public static final String REQUEST_CONTINGENCY = "contingency";

    public static final String REQUEST_ORDINARY_CONTINGENCY = "ordinaryContingency";

    public static final String REQUEST_EXCEPTIONAL_CONTINGENCY = "exceptionalContingency";

    public static final String REQUEST_OUT_OF_RANGE_CONTINGENCY = "outOfRangeContingency";

    public static final String REQUEST_CONTINGENCY_EQUIPMENT = "contingencyEquipment";

    public static final String REQUEST_CONTINGENCIES_NAME = "name";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR = "idEquipmentOperator";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_ID = "contingencyEquipmentId";

    public static final String REQUEST_CONTINGENCIES_MUST_STUDY = "normalMustStudy";

    public static final String REQUEST_CONTINGENCIES_CONTINGENT_STATUS = "contingentStatus";

    public static final String IMPORTED_CONTINGENT_STATUS = "http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService";

    public static final String CONTINGENCY_FILE_KEYWORD = "CO";

    /**
     * requests for flow cnec
     */

    public static final String REQUEST_ASSESSED_ELEMENT = "assessedElement";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY = "assessedElementWithContingency";

    public static final String REQUEST_CURRENT_LIMIT = "currentLimit";

    public static final String REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE = "inBaseCase";

    public static final String REQUEST_ASSESSED_ELEMENT_NAME = "name";

    public static final String REQUEST_ASSESSED_ELEMENT_OPERATOR = "assessedSystemOperator";

    public static final String REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT = "operationalLimit";

    public static final String REQUEST_ASSESSED_ELEMENT_IS_CRITICAL = "isCritical";

    public static final String REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED = "normalEnabled";

    public static final String REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY = "isCombinableWithContingency";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND = "combinationConstraintKind";

    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED = "normalEnabled";

    public static final String REQUEST_CURRENT_LIMIT_NORMAL_VALUE = "normalValue";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_SET = "operationalLimitSet";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TYPE = "operationalLimitType";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_TERMINAL = "terminal";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_KIND = "kind";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_DIRECTION = "direction";

    public static final String REQUEST_CURRENT_LIMIT_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION = "acceptableDuration";

    public static final String ASSESSED_ELEMENT_FILE_KEYWORD = "AE";

    public static final String ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_CONSIDERED = "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.considered";

    public static final String ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_INCLUDED = "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included";

    public static final String ASSESSED_ELEMENT_WITH_CONTINGENCIES_LINK_EXCLUDED = "http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.excluded";

    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary_1");

    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT = List.of("CGMES.Terminal2", "CGMES.Terminal_Boundary_2");

    public static final String OPERATIONAL_LIMIT_TYPE_PATL = "http://iec.ch/TC57/CIM100-European#LimitKind.patl";

    public static final String OPERATIONAL_LIMIT_TYPE_TATL = "http://iec.ch/TC57/CIM100-European#LimitKind.tatl";

    public static final String OPERATIONAL_LIMIT_TYPE_DIRECTION_ABSOLUTE = "http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.absoluteValue";

    public static final String OPERATIONAL_LIMIT_TYPE_DIRECTION_HIGH = "http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.high";

    public static final String OPERATIONAL_LIMIT_TYPE_DIRECTION_LOW = "http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.low";
}
