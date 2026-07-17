/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public enum BorderAreaEICode {
    ES_FR("10YDOM--ES-FR--D", "ES-FR-AREA", TsoEICode.ES, TsoEICode.FR),
    ES_PT("10YDOM--ES-PT--T", "ES-PT-AREA", TsoEICode.ES, TsoEICode.PT);

    private final String eiCode;
    private final String displayName;
    private final TsoEICode area1;
    private final TsoEICode area2;

    BorderAreaEICode(String eiCode, String displayName, TsoEICode area1, TsoEICode area2) {
        this.eiCode = eiCode;
        this.displayName = displayName;
        this.area1 = area1;
        this.area2 = area2;
    }

    public String getEICode() {
        return eiCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<TsoEICode> getAreas() {
        return List.of(area1, area2);
    }

    public static Optional<BorderAreaEICode> fromEICode(String eiCode) {
        return Arrays.stream(BorderAreaEICode.values())
                .filter(tsoEICode -> tsoEICode.eiCode.equals(eiCode))
                .findAny();
    }

}
