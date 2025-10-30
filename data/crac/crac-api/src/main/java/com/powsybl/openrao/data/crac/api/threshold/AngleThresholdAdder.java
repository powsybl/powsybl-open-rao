/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.threshold;

import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface AngleThresholdAdder extends ThresholdAdder<AngleThresholdAdder> {

    AngleCnecAdder add();
}
