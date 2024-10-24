/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;

import java.util.Properties;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneHelper {
    private final Crac crac;
    private final RaoResult raoResult;
    private final boolean relativePositiveMargins;
    private final boolean withLoopFlows;
    private final double mnecAcceptableMarginDiminution;
    private final String documentId;
    private final int revisionNumber;
    private final String domainId;
    private final String processType;
    private final String senderId;
    private final String senderRole;
    private final String receiverId;
    private final String receiverRole;
    private final String timeInterval;

    private CneHelper(Crac crac, RaoResult raoResult, boolean relativePositiveMargins, boolean withLoopFlows,
                      double mnecAcceptableMarginDiminution, String documentId, int revisionNumber, String domainId,
                      String processType, String senderId, String senderRole, String receiverId, String receiverRole,
                      String timeInterval) {
        this.crac = crac;
        this.raoResult = raoResult;
        this.relativePositiveMargins = relativePositiveMargins;
        this.withLoopFlows = withLoopFlows;
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.documentId = documentId;
        this.revisionNumber = revisionNumber;
        this.domainId = domainId;
        this.processType = processType;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.receiverRole = receiverRole;
        this.timeInterval = timeInterval;
    }

    public CneHelper(Crac crac, RaoResult raoResult, Properties properties) {
        this(crac, raoResult,
            Boolean.parseBoolean((String) properties.getOrDefault("relative-positive-margins", "false")),
            Boolean.parseBoolean((String) properties.getOrDefault("with-loop-flows", "false")),
            Double.parseDouble((String) properties.getOrDefault("mnec-acceptable-margin-diminution", "0")),
            properties.getProperty("document-id"),
            Integer.parseInt(properties.getProperty("revision-number")),
            properties.getProperty("domain-id"),
            properties.getProperty("process-type"),
            properties.getProperty("sender-id"),
            properties.getProperty("sender-role"),
            properties.getProperty("receiver-id"),
            properties.getProperty("receiver-role"),
            properties.getProperty("time-interval"));
    }

    public Crac getCrac() {
        return crac;
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public boolean isRelativePositiveMargins() {
        return relativePositiveMargins;
    }

    public boolean isWithLoopFlows() {
        return withLoopFlows;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getProcessType() {
        return processType;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getReceiverRole() {
        return receiverRole;
    }

    public String getTimeInterval() {
        return timeInterval;
    }
}
