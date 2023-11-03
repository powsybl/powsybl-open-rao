package com.farao_community.farao.monitoring.monitoring_common.json;

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
    public static final String PREVENTIVE_INSTANT = "preventive"; // TODO maybe remove them
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_INSTANT = "curative";
}
