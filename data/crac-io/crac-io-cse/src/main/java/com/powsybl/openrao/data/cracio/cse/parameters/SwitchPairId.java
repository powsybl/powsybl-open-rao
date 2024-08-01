/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cse.parameters;

import java.util.Objects;

/***
 * A pair of switch IDs, one to open and one to close.
 * Used to build {@code SwitchPair} remedial actions
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairId {
    private String switchToOpenId;
    private String switchToCloseId;

    public SwitchPairId(String switchToOpenId, String switchToCloseId) {
        Objects.requireNonNull(switchToOpenId);
        Objects.requireNonNull(switchToCloseId);
        this.switchToOpenId = switchToOpenId;
        this.switchToCloseId = switchToCloseId;
    }

    public String getSwitchToOpenId() {
        return switchToOpenId;
    }

    public String getSwitchToCloseId() {
        return switchToCloseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwitchPairId oSwitchPair = (SwitchPairId) o;
        return this.switchToOpenId.equals(oSwitchPair.getSwitchToOpenId()) && this.switchToCloseId.equals(oSwitchPair.getSwitchToCloseId());
    }

    @Override
    public int hashCode() {
        return switchToOpenId.hashCode() + 37 * switchToCloseId.hashCode();
    }
}
