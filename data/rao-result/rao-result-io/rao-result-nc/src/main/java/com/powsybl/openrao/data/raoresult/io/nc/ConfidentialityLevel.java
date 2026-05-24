/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum ConfidentialityLevel {
    OPDE_SECRET("ec8c443e-edab-4256-80b2-4a7824f754ab", "OPDE Secret"),
    OPDE_CONFIDENTIAL("4cd9b326-1275-4da7-9724-28c5e1deeb87", "OPDE Confidential"),
    OPC_STA_CONFIDENTIAL("251e7f88-dbd3-48e0-aea6-669f8cd67e8c", "OPC/STA Confidential"),
    PUBLIC("0080531c-fa18-4d51-8832-4bcbced77c90", "Public"),
    RESTRICTED("ef58471e-08a0-4c8b-b488-3e5ef4a21422", "Restricted"),
    CONFIDENTIAL("51378918-8ef6-4cce-9711-4af4539f1ef5", "Confidential"),
    SENSITIVE("c28079e4-02ba-4a08-9093-8247ebe57f21", "Sensitive");

    private final String identifier;
    private final String name;

    ConfidentialityLevel(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }
}
