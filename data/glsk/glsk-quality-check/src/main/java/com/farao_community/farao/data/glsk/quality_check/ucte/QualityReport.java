/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.quality_check.ucte;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class QualityReport {
    private Map<String, List<QualityLog>> qualityLogsByTso = new TreeMap<>();

    public Map<String, List<QualityLog>> getQualityLogsByTso() {
        return qualityLogsByTso;
    }

    public void info(String checkId, String nodeId, String type, String tso, String message) {
        log(checkId, nodeId, type, tso, SeverityEnum.INFORMATION, message);
    }

    public void warn(String checkId, String nodeId, String type, String tso, String message) {
        log(checkId, nodeId, type, tso, SeverityEnum.WARNING, message);
    }

    public void error(String checkId, String nodeId, String type, String tso, String message) {
        log(checkId, nodeId, type, tso, SeverityEnum.ERROR, message);
    }

    private void log(String checkId, String nodeId, String type, String tso, SeverityEnum severity, String message) {
        qualityLogsByTso.putIfAbsent(tso, new ArrayList<>());
        qualityLogsByTso.get(tso).add(new QualityLog(checkId, nodeId, type, tso, severity, message));
    }

    public List<QualityLog> getAllQualityLogs() {
        return qualityLogsByTso.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<QualityLog> getQualityLogs(String tso) {
        return qualityLogsByTso.getOrDefault(tso, Collections.emptyList());
    }

}
