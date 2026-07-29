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
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RangeActionSensiHandler {

    double getSensitivityOnFlow(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult);

    double getSensitivityOnIntensity(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult);

    void checkConsistency(Network network);

    static RangeActionSensiHandler get(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return new PstRangeActionSensiHandler(pstRangeAction);
        } else if (rangeAction instanceof HvdcRangeAction hvdcRangeAction) {
            return new HvdcRangeActionSensiHandler(hvdcRangeAction);
        } else if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
            return new InjectionRangeActionSensiHandler(injectionRangeAction);
        } else {
            throw new OpenRaoException(String.format("RangeAction implementation %s not handled by sensitivity analysis", rangeAction.getClass()));
        }
    }
}
