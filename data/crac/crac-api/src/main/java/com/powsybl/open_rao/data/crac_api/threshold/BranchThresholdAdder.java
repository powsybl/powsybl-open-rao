/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_api.threshold;

import com.powsybl.open_rao.data.crac_api.cnec.FlowCnecAdder;
import com.powsybl.open_rao.data.crac_api.cnec.Side;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchThresholdAdder extends ThresholdAdder<BranchThresholdAdder> {

    BranchThresholdAdder withSide(Side side);

    FlowCnecAdder add();
}
