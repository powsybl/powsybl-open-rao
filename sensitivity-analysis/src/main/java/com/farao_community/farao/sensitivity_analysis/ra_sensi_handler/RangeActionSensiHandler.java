/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariable;

import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RangeActionSensiHandler {

    List<SensitivityVariable> rangeActionToSensitivityVariable();

    double getSensitivityOnFlow(FlowCnec cnec, SystematicSensitivityResult sensitivityResult);

    void checkConsistency(Network network);

    static RangeActionSensiHandler get(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction) {
            return new PstRangeActionSensiHandler((PstRangeAction) rangeAction);
        } else if (rangeAction instanceof HvdcRangeAction) {
            return new HvdcRangeActionSensiHandler((HvdcRangeAction) rangeAction);
        } else if (rangeAction instanceof InjectionRangeAction) {
            return new InjectionRangeActionSensiHandler((InjectionRangeAction) rangeAction);
        } else {
            throw new SensitivityAnalysisException(String.format("RangeAction implementation %s not handled by sensitivity analysis", rangeAction.getClass()));
        }
    }
}
