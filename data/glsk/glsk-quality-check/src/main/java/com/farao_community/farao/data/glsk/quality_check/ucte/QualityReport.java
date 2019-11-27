package com.farao_community.farao.data.glsk.quality_check.ucte;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class QualityReport {
    private Map<String, List<QualityLog>> qualityLogs = new TreeMap<>();

    public void info(String nodeId, String type, String tso, String message) {
        log(nodeId, type, tso, SeverityEnum.INFORMATION, message);
    }

    public void warn(String nodeId, String type, String tso, String message) {
        log(nodeId, type, tso, SeverityEnum.WARNING, message);
    }

    public void error(String nodeId, String type, String tso, String message) {
        log(nodeId, type, tso, SeverityEnum.ERROR, message);
    }

    private void log(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        qualityLogs.putIfAbsent(tso, new ArrayList<>());
        qualityLogs.get(tso).add(new QualityLog(nodeId, type, tso, severity, message));
    }

    public List<QualityLog> getQualityLogs() {
        return qualityLogs.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<QualityLog> getQualityLogs(String tso) {
        return qualityLogs.getOrDefault(tso, new ArrayList<>());
    }

    public boolean hasQualityLogs(String tso) {
        return qualityLogs.containsKey(tso);
    }

}
