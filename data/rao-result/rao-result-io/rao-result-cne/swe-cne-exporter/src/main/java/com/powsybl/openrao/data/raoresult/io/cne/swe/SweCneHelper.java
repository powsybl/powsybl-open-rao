/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweCneHelper extends CneHelper {
    private Map<Contingency, Boolean> contingencyFailureMap = new HashMap<>();

    public SweCneHelper(Crac crac, RaoResult raoResult, Properties properties) {
        super(crac, raoResult, properties, SweCneUtil.SWE_CNE_EXPORT_PROPERTIES_PREFIX);
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
