/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionAdderProvider.class)
public class LoopFlowThresholdAdderImplProvider implements ExtensionAdderProvider<FlowCnec, LoopFlowThreshold, LoopFlowThresholdAdderImpl> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public Class<LoopFlowThresholdAdderImpl> getAdderClass() {
        return LoopFlowThresholdAdderImpl.class;
    }

    @Override
    public LoopFlowThresholdAdderImpl newAdder(FlowCnec flowCnec) {
        return new LoopFlowThresholdAdderImpl(flowCnec);
    }
}
