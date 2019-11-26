/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class QualityReport {

    private String nodeId;

    private String type;

    private String tso;

    private SeverityEnum severity;

    private String message;

    public static QualityReport setQualityReport(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        return new QualityReport(nodeId, type, tso, severity, message);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getType() {
        return type;
    }

    public String getTso() {
        return tso;
    }

    public SeverityEnum getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public QualityReport(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        this.nodeId = nodeId;
        this.type = type;
        this.tso = tso;
        this.severity = severity;
        this.message = message;
    }
}
