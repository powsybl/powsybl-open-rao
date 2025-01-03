/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalSensitivityAnalysisHelper {
    private InterTemporalRaoInput input;

    public InterTemporalSensitivityAnalysisHelper(InterTemporalRaoInput input) {
        this.input = input;
    }

    public TemporalData<Set<RangeAction<?>>> getRangeActions() {
        Map<OffsetDateTime, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        Set<RangeAction<?>> allRangeActions = new HashSet<>();

        // TODO: see what to do if RAs have same id across timestamps (same object from RemedialAction::equals)
        input.getRaoInputs().getTimestamps().forEach(timestamp -> {
            Crac crac = input.getRaoInputs().getData(timestamp).orElseThrow().getCrac();
            allRangeActions.addAll(crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE, UsageMethod.FORCED));
            rangeActions.put(timestamp, new HashSet<>(allRangeActions));
        });

        return new TemporalDataImpl<>(rangeActions);
    }

    public TemporalData<Set<FlowCnec>> getFlowCnecs() {
        return input.getRaoInputs().map(RaoInput::getCrac).map(crac -> crac.getFlowCnecs().stream().filter(flowCnec -> flowCnec.getState().isPreventive() || flowCnec.getState().getInstant().isOutage()).collect(Collectors.toSet()));
    }
}
