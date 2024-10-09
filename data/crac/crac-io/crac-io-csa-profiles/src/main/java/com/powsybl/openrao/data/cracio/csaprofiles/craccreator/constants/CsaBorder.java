/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum CsaBorder {
    SPAIN_FRANCE("ES-FR", "10YDOM--ES-FR--D"),
    SPAIN_PORTUGAL("ES-PT", "10YDOM--ES-PT--T");

    private final String shortName;
    private final String eiCode;

    CsaBorder(String shortName, String eiCode) {
        this.shortName = shortName;
        this.eiCode = eiCode;
    }

    public String getShortName() {
        return shortName;
    }

    public String getEiCode() {
        return eiCode;
    }
}
