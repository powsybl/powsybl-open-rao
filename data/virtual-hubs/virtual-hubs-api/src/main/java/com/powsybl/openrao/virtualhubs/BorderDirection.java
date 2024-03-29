/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs;

import java.util.Objects;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public class BorderDirection {
    private final String borderFrom;
    private final String borderTo;

    public BorderDirection(String borderFrom, String borderTo) {
        this.borderFrom = Objects.requireNonNull(borderFrom, "BorderDirection creation does not allow null borderFrom");
        this.borderTo = Objects.requireNonNull(borderTo, "BorderDirection creation does not allow null borderTo");
    }

    public String getBorderFrom() {
        return borderFrom;
    }

    public String getBorderTo() {
        return borderTo;
    }
}
