/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import java.time.Instant;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class EndInstantRaoStopCondition implements RaoStopCondition {
    private final Instant targetEndInstant;

    public EndInstantRaoStopCondition(Instant targetEndInstant) {
        this.targetEndInstant = targetEndInstant;
    }

    @Override
    public boolean shouldStop() {
        return targetEndInstant.isBefore(Instant.now());
    }
}
