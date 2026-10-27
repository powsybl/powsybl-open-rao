/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class RaoSecurityStatusHolder {
    private final AtomicBoolean isFlowSecure;
    private final AtomicBoolean isAngleSecure;
    private final AtomicBoolean isVoltageSecure;

    public RaoSecurityStatusHolder() {
        this.isFlowSecure = new AtomicBoolean(true);
        this.isAngleSecure = new AtomicBoolean(true);
        this.isVoltageSecure = new AtomicBoolean(true);
    }

    public boolean getFlowSecurityStatus() {
        return isFlowSecure.get();
    }

    public void updateFlowSecurityStatus(boolean isFlowSecure) {
        this.isFlowSecure.set(isFlowSecure);
    }

    public boolean getAngleSecurityStatus() {
        return isAngleSecure.get();
    }

    public void updateAngleSecurityStatus(boolean isAngleSecure) {
        this.isAngleSecure.set(isAngleSecure);
    }

    public boolean getVoltageSecurityStatus() {
        return isVoltageSecure.get();
    }

    public void updateVoltageSecurityStatus(boolean isVoltageSecure) {
        this.isVoltageSecure.set(isVoltageSecure);
    }
}
