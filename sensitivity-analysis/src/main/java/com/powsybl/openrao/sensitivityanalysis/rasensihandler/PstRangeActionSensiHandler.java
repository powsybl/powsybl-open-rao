/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PstRangeActionSensiHandler implements RangeActionSensiHandler {

    private final PstRangeAction pstRangeAction;

    public PstRangeActionSensiHandler(PstRangeAction pstRangeAction) {
        this.pstRangeAction = pstRangeAction;
    }

    @Override
    public double getSensitivityOnFlow(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnFlow(pstRangeAction.getNetworkElement().getId(), cnec, side);
    }

    @Override
    public double getSensitivityOnIntensity(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnIntensity(pstRangeAction.getNetworkElement().getId(), cnec, side);
    }

    @Override
    public void checkConsistency(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(pstRangeAction.getNetworkElement().getId());
        if (!(identifiable instanceof TwoWindingsTransformer)) {
            throw new OpenRaoException(String.format("Unable to create sensitivity variable for PstRangeAction %s, on element %s", pstRangeAction.getId(), pstRangeAction.getNetworkElement().getId()));
        }
    }
}
