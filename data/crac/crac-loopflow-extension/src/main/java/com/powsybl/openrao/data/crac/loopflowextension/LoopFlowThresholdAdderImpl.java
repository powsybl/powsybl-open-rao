/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowThresholdAdderImpl extends AbstractExtensionAdder<FlowCnec, LoopFlowThreshold> implements LoopFlowThresholdAdder {

    private Double thresholdValue;
    private Unit thresholdUnit;

    public LoopFlowThresholdAdderImpl(FlowCnec flowCnec) {
        super(flowCnec);
    }

    @Override
    public LoopFlowThresholdAdder withValue(double thresholdValue) {
        this.thresholdValue = thresholdValue;
        return this;
    }

    @Override
    public LoopFlowThresholdAdder withUnit(Unit thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
        return this;
    }

    @Override
    protected LoopFlowThreshold createExtension(FlowCnec flowCnec) {
        LoopFlowThresholdUtils.checkAttributes(thresholdValue, thresholdUnit);
        return new LoopFlowThresholdImpl(thresholdValue, thresholdUnit);
    }
}
