/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.util.LoadFlowService;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class ReferenceProgramBuilder {

    private ReferenceProgramBuilder() {

    }

    private static void computeRefFlowOnCurrentNetwork(Network network) {
        // we need this separate load flow to get reference flow on cnec.
        // because reference flow from sensi is not yet fully implemented in powsybl
        //Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        // 1. pre
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId);
        if (loadFlowResult.isOk()) {
            //buildFlowFromNetwork(network, crac, cnecFlowMap);
            int x = 0;
        }
        //return cnecFlowMap;
    }

    /*private static void buildFlowFromNetwork(Network network, Crac crac, Map<Cnec, Double> cnecFlowMap) {
        Set<State> states = new HashSet<>();
        states.add(crac.getPreventiveState());
        states.forEach(state -> crac.getCnecs(state).forEach(cnec -> cnecFlowMap.put(cnec, cnec.getP(network))));
    }*/

    public static ReferenceProgram buildReferenceProgram(Network network) {
        computeRefFlowOnCurrentNetwork(network);
        Map<Country, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        netPositions.forEach((country, flow) -> {
            referenceExchangeDataList.add(new ReferenceExchangeData(country, null, flow));
        });
        return new ReferenceProgram(referenceExchangeDataList);
    }
}
