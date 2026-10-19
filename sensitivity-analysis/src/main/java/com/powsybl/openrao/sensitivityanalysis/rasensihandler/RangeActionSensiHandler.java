/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.*;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RangeActionSensiHandler {

    double getSensitivityOnFlow(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult);

    double getSensitivityOnIntensity(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult);

    void checkConsistency(Network network);

    static RangeActionSensiHandler get(RangeAction<?> rangeAction) {
        return switch (rangeAction) {
            case PstRangeAction pstRangeAction -> new PstRangeActionSensiHandler(pstRangeAction);
            case HvdcRangeAction hvdcRangeAction -> new HvdcRangeActionSensiHandler(hvdcRangeAction);
            case InjectionRangeAction injectionRangeAction -> new InjectionRangeActionSensiHandler(injectionRangeAction);
            case CounterTradeRangeAction counterTradeRangeAction -> new CounterTradeRangeActionSensiHandler(counterTradeRangeAction);
            default -> throw new OpenRaoException(String.format("RangeAction implementation %s not handled by sensitivity analysis", rangeAction.getClass()));
        };
    }
}
