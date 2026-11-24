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
        // in this first implementation we assume no loads so that all generators in the zone sum up to 1
        return sensitivityResult.getSensitivityOnFlow(getPositiveGlskMapId(), cnec, side)
                - sensitivityResult.getSensitivityOnFlow(getNegativeGlskMapId(), cnec, side);
    }

    @Override
    public double getSensitivityOnIntensity(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        // in this first implementation we assume no loads so that all generators in the zone sum up to 1
        return sensitivityResult.getSensitivityOnIntensity(getPositiveGlskMapId(), cnec, side)
            - sensitivityResult.getSensitivityOnIntensity(getNegativeGlskMapId(), cnec, side);
    }

    @Override
    public void checkConsistency(Network network) {
        // Consistency is ok?
    }

    public String getPositiveGlskMapId() {
        return counterTradeRangeAction.getId() + POSITIVE_GLSK_SUFFIX;
    }

    public String getNegativeGlskMapId() {
        return counterTradeRangeAction.getId() + NEGATIVE_GLSK_SUFFIX;
    }
}
