package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class QualityReport {
    private List<QualityLog> qualityLogs = new ArrayList<>();

    public void log(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        qualityLogs.add(new QualityLog(nodeId, type, tso, severity, message));
    }

    public List<QualityLog> getQualityLogs() {
        return qualityLogs;
    }
}
