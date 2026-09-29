/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public interface ConnectedAreaBorderRangeAdder {

    ConnectedAreaBorderRangeAdder withMin(double minSetpoint);

    ConnectedAreaBorderRangeAdder withMax(double maxSetpoint);

    ConnectedAreaBorderRangeAdder withRangeType(RangeType rangeType);

    ConnectedAreaAdder add();
}
