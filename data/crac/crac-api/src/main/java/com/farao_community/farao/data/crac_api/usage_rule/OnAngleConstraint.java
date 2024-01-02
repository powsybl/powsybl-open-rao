/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;

/**
 * The OnAngleConstraint UsageRule is defined on a given AngleCnec. For instance, if a RemedialAction
 * has a OnAngleConstraint UsageRule with State "cnec1" and UsageMethod AVAILABLE, this
 * RemedialAction will only be available if "cnec1" is constrained (= has a negative margin).
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnAngleConstraint extends UsageRule {
    /**
     * Get the AngleCnec that should be constrained
     */
    AngleCnec getAngleCnec();

    /**
     * Get the Instant of the free to use
     */
    Instant getInstant();
}
