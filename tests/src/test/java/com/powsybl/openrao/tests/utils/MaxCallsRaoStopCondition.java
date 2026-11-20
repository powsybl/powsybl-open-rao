/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.utils;

import com.powsybl.openrao.raoapi.RaoStopCondition;

/**
 * Helper RAO Stop Condition that triggers an interruption after a certain number of calls.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaxCallsRaoStopCondition implements RaoStopCondition {
    private int maxCalls;

    public MaxCallsRaoStopCondition(int maxCalls) {
        this.maxCalls = maxCalls;
    }

    @Override
    public boolean shouldStop() {
        maxCalls--;
        return maxCalls <= 0;
    }
}
