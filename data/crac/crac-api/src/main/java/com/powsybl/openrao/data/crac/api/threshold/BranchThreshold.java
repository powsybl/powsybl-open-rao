/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.threshold;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;

/**
 * Specific {@link Threshold} for {@link BranchCnec}
 *
 * a BranchThreshold has a BranchThresholdRule, which defines on each side of the
 * branch the threshold applies.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchThreshold extends Threshold {
    /**
     * Get the {@link TwoSides} of the Branch on which the threshold is defined, which is
     * implicitly deduced from the rule of the threshold
     */
    TwoSides getSide();
}
