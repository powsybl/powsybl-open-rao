/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public interface ConnectedAreaAdder {

    ConnectedAreaAdder withArea(String area);

    ConnectedAreaBorderRangeAdder newBorderRange();

    CounterTradeRangeActionAdder add();
}
