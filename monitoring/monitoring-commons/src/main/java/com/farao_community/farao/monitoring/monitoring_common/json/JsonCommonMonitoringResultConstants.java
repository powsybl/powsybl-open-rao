package com.farao_community.farao.monitoring.monitoring_common.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;

public final class JsonCommonMonitoringResultConstants {
    private JsonCommonMonitoringResultConstants() {
    }

    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String CNEC_ID = "cnec-id";
    public static final String CONTINGENCY = "contingency";
    public static final String INSTANT = "instant";
    public static final String REMEDIAL_ACTIONS = "remedial-actions";

    // instants
    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_INSTANT = "curative";

    public static Instant deserializeInstant(String stringValue) {
        switch (stringValue) {
            case PREVENTIVE_INSTANT:
                return Instant.PREVENTIVE;
            case OUTAGE_INSTANT:
                return Instant.OUTAGE;
            case AUTO_INSTANT:
                return Instant.AUTO;
            case CURATIVE_INSTANT:
                return Instant.CURATIVE;
            default:
                throw new FaraoException(String.format("Unrecognized instant %s", stringValue));
        }
    }
}
