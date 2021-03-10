/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.raw_crac_api.RawCrac;

/**
 * Common interface for CracCreationContext
 *
 * A CracCreationContext contains information on how a {@link Crac} has been created
 * from a {@link RawCrac}.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface CracCreationContext<T extends RawCrac> {
}
