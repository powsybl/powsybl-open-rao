/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public interface CnecResult<I extends Cnec<?>> {
    I getCnec();

    default String getId() {
        return getCnec().getId();
    }

    Cnec.SecurityStatus getCnecSecurityStatus();

    double getMargin();

    String print();
}
