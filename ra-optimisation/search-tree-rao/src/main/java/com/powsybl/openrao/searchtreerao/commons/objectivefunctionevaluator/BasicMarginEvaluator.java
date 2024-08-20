/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

/**
 * It enables to evaluate the absolute margin of a FlowCnec
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class BasicMarginEvaluator implements MarginEvaluator {

    public BasicMarginEvaluator() {
        // do nothing : only getMargin is useful in this class
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Unit unit) {
        return flowResult.getMargin(flowCnec, unit);
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return flowResult.getMargin(flowCnec, side, unit);
    }
}
