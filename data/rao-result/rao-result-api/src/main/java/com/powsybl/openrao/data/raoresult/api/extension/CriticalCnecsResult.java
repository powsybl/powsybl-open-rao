/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class CriticalCnecsResult extends AbstractExtension<RaoResult> {
    Set<String> criticalCnecIds;

    @Override
    public String getName() {
        return "critical-cnecs-result";
    }

    public CriticalCnecsResult(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }

    public CriticalCnecsResult() {
        this.criticalCnecIds = new HashSet<>();
    }

    public Set<String> getCriticalCnecIds() {
        return criticalCnecIds;
    }

    public void setCriticalCnecIds(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }
}
