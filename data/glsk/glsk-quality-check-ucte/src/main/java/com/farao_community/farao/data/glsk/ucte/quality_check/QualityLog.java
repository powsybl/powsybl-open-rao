/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte.quality_check;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class QualityLog {

    private String checkId;
    private String nodeId;
    private String type;
    private String tso;
    private SeverityEnum severity;
    private String message;

    public String getCheckId() {
        return checkId;
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

    public QualityLog(String checkId, String nodeId, String type, String tso, SeverityEnum severity, String message) {
        this.checkId = checkId;
        this.nodeId = nodeId;
        this.type = type;
        this.tso = tso;
        this.severity = severity;
        this.message = message;
    }
}
