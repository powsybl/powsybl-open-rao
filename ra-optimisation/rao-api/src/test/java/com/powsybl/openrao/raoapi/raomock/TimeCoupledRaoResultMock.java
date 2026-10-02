/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.raomock;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledRaoResultMock extends AbstractExtendable<RaoResult> implements TimeCoupledRaoResult {
    @Override
    public List<OffsetDateTime> getTimestamps() {
        return null;
    }

    @Override
    public RaoResult getIndividualRaoResult(OffsetDateTime timestamp) {
        return null;
    }

    @Override
    public void write(ZipOutputStream zipOutputStream, TemporalData<Crac> cracs, Properties properties) {

    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return null;
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return null;
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return null;
    }
}
