/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class RaoSecurityStatusHolder {
    private final Set<AtomicBoolean> flowStatuses;
    private final Set<AtomicBoolean> angleStatuses;
    private final Set<AtomicBoolean> voltageStatuses;

    public static final RaoSecurityStatusHolder DEFAULT = new RaoSecurityStatusHolder(); // used for placeholder

    public RaoSecurityStatusHolder() {
        this.flowStatuses = new HashSet<>();
        this.angleStatuses = new HashSet<>();
        this.voltageStatuses = new HashSet<>();
    }

    public boolean getSecurityStatus(PhysicalParameter physicalParameter) {
        return switch (physicalParameter) {
            case ANGLE -> reduceBooleanSet(angleStatuses);
            case FLOW -> reduceBooleanSet(flowStatuses);
            case VOLTAGE -> reduceBooleanSet(voltageStatuses);
        };
    }

    public void registerSecurityStatus(AtomicBoolean statusWatcher, PhysicalParameter physicalParameter) {
        switch (physicalParameter) {
            case ANGLE -> this.angleStatuses.add(statusWatcher);
            case FLOW -> this.flowStatuses.add(statusWatcher);
            case VOLTAGE -> this.voltageStatuses.add(statusWatcher);
            default -> throw new OpenRaoException("Unsupported physical parameter: " + physicalParameter);
        }
    }

    private static boolean reduceBooleanSet(Set<AtomicBoolean> booleanSet) {
        return booleanSet.stream().allMatch(AtomicBoolean::get);
    }
}
