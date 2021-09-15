/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

public enum ImportStatus {
    IMPORTED("Import OK."),
    ELEMENT_NOT_FOUND_IN_NETWORK("Not found in network."),
    INCOMPLETE_DATA("Data incomplete"),
    INCONSISTENCY_IN_DATA("Data inconsistent"),
    NOT_YET_HANDLED_BY_FARAO("Functionality is not handled by FARAO for the moment."),
    NOT_FOR_RAO("Not used in RAO"),
    NOT_FOR_REQUESTED_TIMESTAMP("Not for requested timestamp"),
    OTHER("");

    private final String detail;

    public String getDescription() {
        return detail;
    }

    ImportStatus(String detail) {
        this.detail = detail;
    }
}
