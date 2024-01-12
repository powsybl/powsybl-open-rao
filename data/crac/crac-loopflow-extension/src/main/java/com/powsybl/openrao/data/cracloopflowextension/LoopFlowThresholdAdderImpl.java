/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracloopflowextension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.commons.extensions.AbstractExtensionAdder;

import java.util.Objects;

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

        if (Objects.isNull(thresholdValue)) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value");
        }
        if (Objects.isNull(thresholdUnit)) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value");
        }
        if (thresholdValue < 0) {
            throw new OpenRaoException("LoopFlowThresholds must have a positive threshold.");
        }
        if (thresholdUnit.equals(Unit.PERCENT_IMAX) && (thresholdValue > 1 || thresholdValue < 0)) {
            throw new OpenRaoException("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.");
        }
        if (thresholdUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new OpenRaoException("LoopFlowThresholds can only be defined in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        return new LoopFlowThresholdImpl(thresholdValue, thresholdUnit);
    }
}
