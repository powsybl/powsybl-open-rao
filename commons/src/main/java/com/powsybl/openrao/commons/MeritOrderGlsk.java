/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.util.List;

/**
 * Merit-order GLSK of an area. List order is the merit order; a generator may appear in several tranches.
 *
 * @author Rodolphe Griset {@literal <rodolphe.griset at artelys.com>}
 */
public record MeritOrderGlsk(String area, List<MeritOrderTranche> upward, List<MeritOrderTranche> downward) {

    public MeritOrderGlsk {
        upward = List.copyOf(upward);
        downward = List.copyOf(downward);
    }
}
