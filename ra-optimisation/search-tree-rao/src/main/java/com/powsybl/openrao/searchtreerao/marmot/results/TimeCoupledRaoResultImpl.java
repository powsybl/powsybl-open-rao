/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledRaoResultImpl extends AbstractExtendable<RaoResult> implements TimeCoupledRaoResult {
    private final TemporalData<? extends RaoResult> raoResultPerTimestamp;

    private static final String MISSING_RAO_RESULT_ERROR_MESSAGE = "No RAO Result data found for the provided timestamp.";

    public TimeCoupledRaoResultImpl(TemporalData<? extends RaoResult> raoResultPerTimestamp) {
        this.raoResultPerTimestamp = raoResultPerTimestamp;
    }

    @Override
    public List<OffsetDateTime> getTimestamps() {
        return raoResultPerTimestamp.getTimestamps();
    }

    @Override
    public RaoResult getIndividualRaoResult(OffsetDateTime timestamp) {
        return raoResultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException(MISSING_RAO_RESULT_ERROR_MESSAGE));
    }

    @Override
    public void write(ZipOutputStream zipOutputStream, TemporalData<Crac> cracs, Properties properties) throws IOException {
        RaoResultArchiveManager.exportAndZipResults(zipOutputStream, this, cracs, properties);
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).wasActivatedBeforeState(state, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).isActivatedDuringState(state, networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getActivatedNetworkActionsDuringState(state);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).isActivatedDuringState(state, rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getActivatedRangeActionsDuringState(state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedTapsOnState(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedSetPointsOnState(state);
    }
}
