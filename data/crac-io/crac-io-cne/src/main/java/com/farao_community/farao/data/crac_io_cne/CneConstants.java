/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

/**
 * Constants used in the CNE file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneConstants {

    /* Messages */
    public static final String UNHANDLED_UNIT = "Unhandled unit %s";

    /* General */
    public static final String CNE_XSD_2_4 = "iec62325-451-n-cne_v2_4.xsd";
    public static final String LOCALTYPES_XSD = "urn-entsoe-eu-local-extension-types.xsd";
    public static final String CODELISTS_XSD = "urn-entsoe-eu-wgedi-codelists.xsd";
    public static final String CNE_TAG = "CriticalNetworkElement_MarketDocument";
    public static final String CNE_FORMAT = "CNE";

    // codingScheme
    public static final String A01_CODING_SCHEME = "A01";
    public static final String A02_CODING_SCHEME = "A02";

    /* CriticalNetworkElement_MarketDocument */
    // type
    public static final String CNE_TYPE = "B06";
    // process.processType
    public static final String CNE_PROCESS_TYPE = "A48";
    // sender_MarketParticipant.mRID
    public static final String CNE_SENDER_MRID = "22XCORESO------S";
    // sender_MarketParticipant.marketRole.type
    public static final String CNE_SENDER_MARKET_ROLE_TYPE = "A32";
    // receiver_MarketParticipant.mRID
    public static final String CNE_RECEIVER_MRID = "17XTSO-CS------W";
    // receiver_MarketParticipant.marketRole.type
    public static final String CNE_RECEIVER_MARKET_ROLE_TYPE = "A36";

    /* TimeSeries */
    // businessType
    public static final String B54_BUSINESS_TYPE_TS = "B54";
    // curveType
    public static final String A01_CURVE_TYPE = "A01";

    /* Period */
    // resolution
    public static final String SIXTY_MINUTES_DURATION = "PT60M";

    /* Constraint_Series */
    // businessType
    public static final String B54_BUSINESS_TYPE = "B54";
    public static final String B56_BUSINESS_TYPE = "B56";
    public static final String B57_BUSINESS_TYPE = "B57";
    public static final String B88_BUSINESS_TYPE = "B88";
    // optimization_MarketObjectStatus.status
    public static final String OPTIMIZED_MARKET_STATUS = "A52";

    /* Measurements */
    // measurementType
    public static final String FLOW_MEASUREMENT_TYPE = "A01";
    public static final String PATL_MEASUREMENT_TYPE = "A02";
    public static final String FRM_MEASUREMENT_TYPE = "A03";
    public static final String TATL_MEASUREMENT_TYPE = "A07";
    public static final String SUM_PTDF_MEASUREMENT_TYPE = "Z01";
    public static final String ABS_MARG_PATL_MEASUREMENT_TYPE = "Z02";
    public static final String OBJ_FUNC_PATL_MEASUREMENT_TYPE = "Z03";
    public static final String ABS_MARG_TATL_MEASUREMENT_TYPE = "Z04";
    public static final String OBJ_FUNC_TATL_MEASUREMENT_TYPE = "Z05";
    public static final String LOOPFLOW_MEASUREMENT_TYPE = "Z06";
    public static final String MAX_LOOPFLOW_MEASUREMENT_TYPE = "Z07";
    // unitSymbol
    public static final String AMP_UNIT_SYMBOL = "AMP";
    public static final String MAW_UNIT_SYMBOL = "MAW";
    // positiveFlowIn
    public static final String DIRECT_POSITIVE_FLOW_IN = "A01";
    public static final String OPPOSITE_POSITIVE_FLOW_IN = "A02";

    /* RemedialAction_Series */
    // applicationMode_MarketObjectStatus.status
    public static final String PREVENTIVE_MARKET_OBJECT_STATUS = "A18";

    /* RegisteredResource */
    // pSRType.psrType
    public static final String PST_RANGE_PSR_TYPE = "A06";
    // resourceCapacity.unitSymbol
    public static final String WITHOUT_UNIT_SYMBOL = "C62";
    // marketObjectStatus.status
    public static final String ABSOLUTE_MARKET_OBJECT_STATUS = "A26";

    /* Reason */
    // code
    public static final String SECURE_REASON_CODE = "Z13";
    public static final String UNSECURE_REASON_CODE = "Z03";
    public static final String OTHER_FAILURE_REASON_CODE = "999";
    // type
    public static final String SECURE_REASON_TEXT = "Situation is secure";
    public static final String UNSECURE_REASON_TEXT = "Situation is unsecure";
    public static final String OTHER_FAILURE_REASON_TEXT = "Other failure";

    private CneConstants() { }
}
