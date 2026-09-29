/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

import java.util.List;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public interface ConnectedArea {
    String getArea();

    List<StandardRange> getBorderRanges();
}
