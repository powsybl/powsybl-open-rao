/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum NcKeyword {
    ASSESSED_ELEMENT("AE", "AssessedElement"),
    CONTINGENCY("CO", "Contingency"),
    EQUIPMENT_RELIABILITY("ER", "EquipmentReliability"),
    REMEDIAL_ACTION("RA", "RemedialAction"),
    STEADY_STATE_INSTRUCTION("SSI", "SteadyStateInstruction"),
    STEADY_STATE_HYPOTHESIS("SSH", "SteadyStateHypothesis"),
    CGMES("CGMES", "CGMES");

    private final String keyword;
    private final String fullName;

    NcKeyword(String keyword, String fullName) {
        this.keyword = keyword;
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return keyword;
    }

    public String getFullName() {
        return fullName;
    }

    public static NcKeyword getNcKeyword(String keyword) {
        for (NcKeyword ncKeyword : values()) {
            if (ncKeyword.keyword.equals(keyword)) {
                return ncKeyword;
            }
        }
        return null;
    }
}
