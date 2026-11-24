/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface RaoStopCondition {
    /**
     * @return true if the computation should stop at the next checkpoint
     */
    boolean shouldStop();

    /**
     * To be called at checkpoints; throws a dedicated exception if shouldStop() is true
     */
    default void checkNotStopped() {
        if (shouldStop()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("RAO interruption.");
            throw new RaoInterruptedException();
        }
    }

    RaoStopCondition NO_OP = () -> false;
}
