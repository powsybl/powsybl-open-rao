/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class TopologyChanger {

    private TopologyChanger() {}

    public static void applyPreventiveNetworkActions(TemporalData<RaoInput> raoInputs, TemporalData<RaoResult> raoResults){
        raoInputs.getDataPerTimestamp().forEach(((timestamp, raoInput) -> {
            String currentNetworkVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
            String newNetworkVariantId = currentNetworkVariantId+"_with_topological_actions";
            raoInput.getNetwork().getVariantManager().cloneVariant(currentNetworkVariantId, newNetworkVariantId);
            raoInput.getNetwork().getVariantManager().setWorkingVariant(newNetworkVariantId);
            raoResults.getData(timestamp).get().getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState())
                .forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
        }));
    }
}
