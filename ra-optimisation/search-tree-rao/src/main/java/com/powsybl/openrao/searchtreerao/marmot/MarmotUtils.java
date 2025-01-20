/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class MarmotUtils {

    private MarmotUtils() {
    }

    public static Set<FlowCnec> getPreventivePerimeterCnecs(Crac crac) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(crac.getPreventiveState());
        crac.getStates(crac.getInstant(InstantKind.OUTAGE)).forEach(state -> flowCnecs.addAll(crac.getFlowCnecs(state)));
        return flowCnecs;
    }
}
