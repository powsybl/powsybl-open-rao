/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.commons.extensions.Extension;

/**
 * todo: javadoc
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface LoopFlowThreshold extends Extension<FlowCnec> {

    @Override
    default String getName() {
        return "LoopFlowThreshold";
    }

    public double getValue();

    public Unit getUnit();

    public double getThreshold(Unit requestedUnit);

    public double getThresholdWithReliabilityMargin(Unit requestedUnit);

}
