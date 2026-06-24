/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PreTimeCouplingOverloadedCnecs extends AbstractExtension<RaoResult> {
    Set<String> criticalCnecIds;

    @Override
    public String getName() {
        return "pre-time-coupling-overloaded-cnecs";
    }

    public PreTimeCouplingOverloadedCnecs(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }

    public PreTimeCouplingOverloadedCnecs() {
        this.criticalCnecIds = new HashSet<>();
    }

    public Set<String> getCriticalCnecIds() {
        return criticalCnecIds;
    }

    public void setCriticalCnecIds(Set<String> criticalCnecIds) {
        this.criticalCnecIds = criticalCnecIds;
    }
}
