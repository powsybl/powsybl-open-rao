/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.Map;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
public class CounterTradeRangeActionSensiHandler implements RangeActionSensiHandler {

    private static final String POSITIVE_GLSK_SUFFIX = "-positiveInjections";
    private static final String NEGATIVE_GLSK_SUFFIX = "-negativeInjections";
    private final CounterTradeRangeAction counterTradeRangeAction;

    public CounterTradeRangeActionSensiHandler(CounterTradeRangeAction counterTradeRangeAction) {
        this.counterTradeRangeAction = counterTradeRangeAction;
    }

    @Override
    public double getSensitivityOnFlow(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnFlow(getPositiveGlskMapId(), cnec, side) * getSumOfValues(getPositiveGlskMap())
                - sensitivityResult.getSensitivityOnFlow(getNegativeGlskMapId(), cnec, side) * getSumOfValues(getNegativeGlskMap());
    }

    @Override
    public double getSensitivityOnIntensity(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnIntensity(getPositiveGlskMapId(), cnec, side) * getSumOfValues(getPositiveGlskMap())
            - sensitivityResult.getSensitivityOnIntensity(getNegativeGlskMapId(), cnec, side) * getSumOfValues(getNegativeGlskMap());
    }

    @Override
    public void checkConsistency(Network network) {
        // Consistency is ok?
    }

    public Map<String, Float> getPositiveGlskMap() {
        // TODO: how to get GLSK from here?
        return Map.of("country", (float) 1.);
    }

    public Map<String, Float> getNegativeGlskMap() {
        // TODO: how to get GLSK from here?
        return Map.of("country", (float) 1.);
    }

    private double getSumOfValues(Map<String, Float> glskMap) {
        return glskMap.values().stream()
                .mapToDouble(v -> v)
                .sum();
    }

    public String getPositiveGlskMapId() {
        return counterTradeRangeAction.getId() + POSITIVE_GLSK_SUFFIX;
    }

    public String getNegativeGlskMapId() {
        return counterTradeRangeAction.getId() + NEGATIVE_GLSK_SUFFIX;
    }
}
