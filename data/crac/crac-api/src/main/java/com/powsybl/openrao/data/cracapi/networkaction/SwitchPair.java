/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.networkaction;

import com.powsybl.openrao.data.cracapi.NetworkElement;

/***
 * A SwitchPair action is an Elementary Action which consists in changing
 * closing one switch and opening the other.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface SwitchPair extends ElementaryAction {
    /**
     * Get the switch that should be opened by the elementary action
     */
    NetworkElement getSwitchToOpen();

    /**
     * Get the switch that should be closed by the elementary action
     */
    NetworkElement getSwitchToClose();

    @Override
    default boolean isCompatibleWith(ElementaryAction otherElementaryAction) {
        if (otherElementaryAction instanceof SwitchPair switchPair) {
            return getSwitchToOpen().equals(switchPair.getSwitchToOpen()) && getSwitchToClose().equals(switchPair.getSwitchToClose())
                || !getSwitchToOpen().equals(switchPair.getSwitchToOpen()) && !getSwitchToOpen().equals(switchPair.getSwitchToClose()) && !getSwitchToClose().equals(switchPair.getSwitchToClose()) && !getSwitchToClose().equals(switchPair.getSwitchToOpen());
        }
        return true;
    }
}
