/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.networkaction;

import com.powsybl.action.MultipleActionsAction;
import com.powsybl.action.SwitchAction;
import java.util.List;

/**
 * A SwitchPair action is an Elementary Action which consists in changing
 * closing one switch and opening the other.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPair extends MultipleActionsAction {

    private final String switchToOpen;
    private final String switchToClose;

    public SwitchPair(String id, String switchToOpen, String switchToClose) {
        super(id,
            List.of(
                new SwitchAction(id + " - open", switchToOpen, true),
                new SwitchAction(id + " - close", switchToClose, false)
            ));
        this.switchToOpen = switchToOpen;
        this.switchToClose = switchToClose;
    }

    /**
     * Get the switch that should be opened by the elementary action
     */
    public String getSwitchToOpenId() {
        return switchToOpen;
    }

    /**
     * Get the switch that should be closed by the elementary action
     */
    public String getSwitchToCloseId() {
        return switchToClose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwitchPair oSwitchPair = (SwitchPair) o;
        return this.getSwitchToOpenId().equals(oSwitchPair.getSwitchToOpenId()) && this.getSwitchToCloseId().equals(oSwitchPair.getSwitchToCloseId());
    }

    @Override
    public int hashCode() {
        return switchToOpen.hashCode() + 37 * switchToClose.hashCode();
    }

}
