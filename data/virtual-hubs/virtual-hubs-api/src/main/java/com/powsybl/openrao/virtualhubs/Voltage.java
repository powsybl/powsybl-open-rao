/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public record Voltage(BigDecimal nominal, BigDecimal initial, int slackSearchPriority) {
    public Voltage(final BigDecimal nominal, final BigDecimal initial, final int slackSearchPriority) {
        this.nominal = Objects.requireNonNull(nominal, "Voltage creation does not allow null attribute 'nominal'");
        this.initial = Objects.requireNonNull(initial, "Voltage creation does not allow null attribute 'initial'");
        this.slackSearchPriority = slackSearchPriority;
    }
}
