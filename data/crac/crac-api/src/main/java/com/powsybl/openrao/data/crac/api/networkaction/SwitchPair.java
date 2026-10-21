/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.networkaction;

import com.powsybl.action.Action;
import com.powsybl.openrao.data.crac.api.NetworkElement;

/**
 * A SwitchPair action is an Elementary Action which consists in changing
 * closing one switch and opening the other.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface SwitchPair extends Action {
    /**
     * Get the switch that should be opened by the elementary action
     *
     * @deprecated since 7.4.0, use {@link #getSwitchToOpenId()} instead
     */
    @Deprecated(since = "7.4.0")
    NetworkElement getSwitchToOpen();

    /**
     * Get the switch that should be opened by the elementary action
     */
    String getSwitchToOpenId();

    /**
     * Get the switch that should be closed by the elementary action
     *
     * @deprecated since 7.4.0, use {@link #getSwitchToCloseId()} instead
     */
    @Deprecated(since = "7.4.0")
    NetworkElement getSwitchToClose();

    /**
     * Get the switch that should be closed by the elementary action
     */
    String getSwitchToCloseId();

    default boolean isCompatibleWith(Action otherElementaryAction) {
        if (otherElementaryAction instanceof SwitchPair switchPair) {
            return getSwitchToOpenId().equals(switchPair.getSwitchToOpenId())
                    && getSwitchToCloseId().equals(switchPair.getSwitchToCloseId())
                || !getSwitchToOpenId().equals(switchPair.getSwitchToOpenId())
                    && !getSwitchToOpenId().equals(switchPair.getSwitchToCloseId())
                    && !getSwitchToCloseId().equals(switchPair.getSwitchToCloseId())
                    && !getSwitchToCloseId().equals(switchPair.getSwitchToOpenId());
        }
        return true;
    }
}
