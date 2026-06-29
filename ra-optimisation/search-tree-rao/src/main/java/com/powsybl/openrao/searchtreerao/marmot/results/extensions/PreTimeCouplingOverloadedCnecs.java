/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results.extensions;

import com.powsybl.openrao.data.raoresult.api.extension.AbstractCnecIdsExtension;

import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PreTimeCouplingOverloadedCnecs extends AbstractCnecIdsExtension {

    @Override
    public String getName() {
        return "pre-time-coupling-overloaded-cnecs";
    }

    public PreTimeCouplingOverloadedCnecs(Set<String> criticalCnecIds) {
        super(criticalCnecIds);
    }

    public PreTimeCouplingOverloadedCnecs() {
        super();
    }
}
