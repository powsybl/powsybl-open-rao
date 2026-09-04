/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.range.StandardRange;

import java.util.List;

/**
 * An area connected to a {@link CounterTradeRangeAction}'s area, with the range of the border between them.
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public interface ConnectedArea {

    /**
     * Get the connected area
     */
    String getArea();

    /**
     * Get the ranges of the border with the connected area
     */
    List<StandardRange> getBorderRanges();
}
