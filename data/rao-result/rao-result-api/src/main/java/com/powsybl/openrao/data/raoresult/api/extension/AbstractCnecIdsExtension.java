/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public abstract class AbstractCnecIdsExtension extends AbstractExtension<RaoResult> {
    private Set<String> criticalCnecIds;

    protected AbstractCnecIdsExtension(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }

    protected AbstractCnecIdsExtension() {
        this.criticalCnecIds = new HashSet<>();
    }

    public Set<String> getCriticalCnecIds() {
        return criticalCnecIds;
    }

    public void setCriticalCnecIds(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }
}
