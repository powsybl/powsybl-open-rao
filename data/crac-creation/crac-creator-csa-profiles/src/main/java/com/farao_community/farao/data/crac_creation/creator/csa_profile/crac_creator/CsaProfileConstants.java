/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    private CsaProfileConstants() {
    }

    /**
     * constants to read rdf files
     */

    public static final String RDF_FORMAT_CSA_PROFILE = "ApplicationProfiles_NC_RDFS_SHACL_v2_2_v28Mar2023_UMLv90.zip";
    //public static final String RDF_FORMAT_CSA_PROFILE = "RDFS/ContingencyProfile_v2_2_RDFSv2020_24Mar2023.rdf";

    public static final String RDF_BASE_URL = "http://entsoe.eu";

    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";

    /**
     * constants to access triplestore data
     */

    public static final String SPARQL_FILE_CSA_PROFILE = "csa_profile.sparql";

    public static final String REQUEST_CONTINGENCIES = "contingencies";

    public static final String REQUEST_CONTINGENCIES_RDFID = "rdfId";

    public static final String REQUEST_CONTINGENCIES_NAME = "name";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR = "idEquipmentOperator";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT = "contingencyEquipmentId";

    public static final String REQUEST_CONTINGENCIES_MUST_STUDY = "normalMustStudy";

    public static final String REQUEST_CONTINGENCIES_CONTINGENT_STATUS = "contingentStatus";

    public static final String IMPORTED_CONTINGENT_STATUS = "http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService";

}
