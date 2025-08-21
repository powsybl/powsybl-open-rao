/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.HashSet;
import java.util.Set;

/**
 * This class aggregates SensitivityResult stored in TemporalData<SensitivityResult> in one big SensitivityResult.
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GlobalSensitivityResult extends AbstractGlobalResult<SensitivityResult> implements SensitivityResult {

    public GlobalSensitivityResult(TemporalData<SensitivityResult> sensitivityResultPerTimestamp) {
        super(sensitivityResultPerTimestamp);
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        return MarmotUtils.getGlobalComputationStatus(resultPerTimestamp, SensitivityResult::getSensitivityStatus);
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getSensitivityStatus(state);
    }

    @Override
    public Set<String> getContingencies() {
        Set<String> allContingencies = new HashSet<>();
        resultPerTimestamp.map(SensitivityResult::getContingencies).getDataPerTimestamp().values().forEach(allContingencies::addAll);
        return allContingencies;
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getSensitivityValue(flowCnec, side, rangeAction, unit);
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, flowCnec.getState()).getSensitivityValue(flowCnec, side, linearGlsk, unit);
    }
}
