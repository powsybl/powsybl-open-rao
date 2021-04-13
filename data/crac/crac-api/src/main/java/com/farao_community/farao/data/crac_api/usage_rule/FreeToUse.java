/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.Instant;

/**
 * The FreeToUse UsageRule is defined at a given Instant. For instance, if a RemedialAction
 * have a FreeToUse UsageRule with Instant "curative" and UsageMethod AVAILABLE, this
 * RemedialAction will be available after all the contingencies at Instant "curative".
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface FreeToUse extends UsageRule {
    Instant getInstant();
}
