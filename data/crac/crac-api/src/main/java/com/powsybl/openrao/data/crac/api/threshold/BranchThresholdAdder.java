/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.threshold;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchThresholdAdder extends ThresholdAdder<BranchThresholdAdder> {

    BranchThresholdAdder withSide(TwoSides side);

    FlowCnecAdder add();
}
