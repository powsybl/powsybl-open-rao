/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

/**
 * The OnState UsageRule is defined on a given State. For instance, if a RemedialAction
 * have a OnState UsageRule with State "curative-co1" and UsageMethod FORCED, this
 * RemedialAction will be forced in the State "curative-co1".
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface OnState extends UsageRule {
    State getState();

    Contingency getContingency();

    Instant getInstant();
}
