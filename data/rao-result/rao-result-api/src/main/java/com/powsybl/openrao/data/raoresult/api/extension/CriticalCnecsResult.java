/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import java.util.Set;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class CriticalCnecsResult extends AbstractCnecIdsExtension {

    @Override
    public String getName() {
        return "critical-cnecs-result";
    }

    public CriticalCnecsResult(Set<String> criticalCnecIds) {
        super(criticalCnecIds);
    }

    public CriticalCnecsResult() {
        super();
    }
}
