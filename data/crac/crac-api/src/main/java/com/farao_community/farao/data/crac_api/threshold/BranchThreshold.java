/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.threshold;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;

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
     * Get the {@link BranchThresholdRule} of the threshold
     */
    BranchThresholdRule getRule();

    /**
     * Get the {@link Side} of the Branch on which the threshold is defined, which is
     * implicitly deduced from the rule of the threshold
     */
    Side getSide();
}
