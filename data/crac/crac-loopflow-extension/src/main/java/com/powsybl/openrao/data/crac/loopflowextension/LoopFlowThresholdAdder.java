/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.commons.extensions.ExtensionAdder;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface LoopFlowThresholdAdder extends ExtensionAdder<FlowCnec, LoopFlowThreshold> {

    @Override
    default Class<LoopFlowThreshold> getExtensionClass() {
        return LoopFlowThreshold.class;
    }

    LoopFlowThresholdAdder withValue(double thresholdValue);

    /**
     * if unit is PERCENT_IMAX, the value should be between 0 and 1, where 1 = 100%.
     */
    LoopFlowThresholdAdder withUnit(Unit thresholdUnit);

}
