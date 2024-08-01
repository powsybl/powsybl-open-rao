/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum CsaProfileKeyword {
    ASSESSED_ELEMENT("AE"),
    CONTINGENCY("CO"),
    EQUIPMENT_RELIABILITY("ER"),
    REMEDIAL_ACTION("RA"),
    STEADY_STATE_INSTRUCTION("SSI"),
    STEADY_STATE_HYPOTHESIS("SSH"),
    CGMES("CGMES");

    private final String keyword;

    CsaProfileKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return keyword;
    }
}
