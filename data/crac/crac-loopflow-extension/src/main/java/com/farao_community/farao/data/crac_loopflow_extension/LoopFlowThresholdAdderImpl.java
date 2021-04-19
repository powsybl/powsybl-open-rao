/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
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
            throw new FaraoException("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value");
        }
        if (Objects.isNull(thresholdUnit)) {
            throw new FaraoException("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value");
        }
        if (thresholdValue < 0) {
            throw new FaraoException("LoopFlowThresholds must have a positive threshold.");
        }
        if (thresholdUnit.equals(Unit.PERCENT_IMAX) && (thresholdValue > 1 || thresholdValue < 0)) {
            throw new FaraoException("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.");
        }

        return new LoopFlowThresholdImpl(thresholdValue, thresholdUnit);
    }
}
