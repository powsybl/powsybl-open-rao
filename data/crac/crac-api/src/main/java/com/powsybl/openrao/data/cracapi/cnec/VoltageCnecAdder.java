/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.cnec;

import com.powsybl.openrao.data.cracapi.threshold.VoltageThresholdAdder;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface VoltageCnecAdder extends CnecAdder<VoltageCnecAdder> {
    VoltageThresholdAdder newThreshold();

    VoltageCnec add();
}
