/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.cne_exporter_commons;

/**
 * Parameters for CNE export
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneExporterParameters {
    private final String documentId;
    private final int revisionNumber;
    private final String domainId;
    private final ProcessType processType;
    private final String senderId;
    private final RoleType senderRole;
    private final String receiverId;
    private final RoleType receiverRole;
    private final String timeInterval;

    public CneExporterParameters(String documentId, int revisionNumber, String domainId, ProcessType processType, String senderId, RoleType senderRole, String receiverId, RoleType receiverRole, String timeInterval) {
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

    public String getDocumentId() {
        return documentId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public String getDomainId() {
        return domainId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public String getSenderId() {
        return senderId;
    }

    public RoleType getSenderRole() {
        return senderRole;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public RoleType getReceiverRole() {
        return receiverRole;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public enum RoleType {
        CAPACITY_COORDINATOR("A36"),
        REGIONAL_SECURITY_COORDINATOR("A44"),
        SYSTEM_OPERATOR("A04");

        private final String code;

        RoleType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return getCode();
        }
    }

    public enum ProcessType {
        DAY_AHEAD_CC("A48"), // Day-ahead capacity determination
        Z01("Z01"); // Day-ahead capacity determination for SWE, does not exist in xsd

        private final String code;

        ProcessType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return getCode();
        }
    }
}
