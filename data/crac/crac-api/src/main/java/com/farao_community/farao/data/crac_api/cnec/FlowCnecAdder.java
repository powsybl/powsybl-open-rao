/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface FlowCnecAdder extends CnecAdder<FlowCnecAdder> {

    FlowCnecAdder withIMax(double iMaxInAmpere);

    FlowCnecAdder withIMax(double iMaxInAmpere, Side side);

    FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt);

    FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt, Side side);

    BranchThresholdAdder newThreshold();

    FlowCnec add();
}
