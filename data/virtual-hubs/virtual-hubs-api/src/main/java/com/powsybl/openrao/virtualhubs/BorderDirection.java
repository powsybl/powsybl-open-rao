/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.Objects;

/**
 * Border direction description POJO
 *
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record BorderDirection(String from, String to, boolean isAhc, boolean isSdac) {
    public BorderDirection(final String from, final String to, final boolean isAhc, final boolean isSdac) {
        this.from = Objects.requireNonNull(from, "BorderDirection creation does not allow null attribute 'from'");
        this.to = Objects.requireNonNull(to, "BorderDirection creation does not allow null attribute 'to'");
        this.isAhc = isAhc;
        this.isSdac = isSdac;
    }
}
