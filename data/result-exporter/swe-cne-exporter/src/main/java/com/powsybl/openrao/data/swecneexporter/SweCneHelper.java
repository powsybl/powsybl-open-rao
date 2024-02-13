/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.openrao.data.cneexportercommons.CneExporterParameters;
import com.powsybl.openrao.data.cneexportercommons.CneHelper;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweCneHelper extends CneHelper {
    private Map<Contingency, Boolean> contingencyFailureMap = new HashMap<>();

    public SweCneHelper(Crac crac, Network network, RaoResult raoResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        super(crac, network, raoResult, raoParameters, exporterParameters);
        defineContingencyFailureMap();
    }

    public boolean isContingencyDivergent(Contingency contingency) {
        return contingencyFailureMap.getOrDefault(contingency, false);
    }

    private void defineContingencyFailureMap() {
        contingencyFailureMap = getCrac().getContingencies().stream()
                .collect(Collectors.toMap(Function.identity(), contingency -> getCrac().getStates(contingency).stream()
                        .anyMatch(state -> getRaoResult().getComputationStatus(state).equals(ComputationStatus.FAILURE))));
    }

    public boolean isAnyContingencyInFailure() {
        return contingencyFailureMap.containsValue(true);
    }
}
