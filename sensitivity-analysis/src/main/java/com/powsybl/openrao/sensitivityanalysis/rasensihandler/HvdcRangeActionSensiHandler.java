/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class HvdcRangeActionSensiHandler implements RangeActionSensiHandler {

    private final HvdcRangeAction hvdcRangeAction;

    public HvdcRangeActionSensiHandler(HvdcRangeAction hvdcRangeAction) {
        this.hvdcRangeAction = hvdcRangeAction;
    }

    @Override
    public double getSensitivityOnFlow(FlowCnec cnec, Side side, SystematicSensitivityResult sensitivityResult, Instant instant) {
        return sensitivityResult.getSensitivityOnFlow(hvdcRangeAction.getNetworkElement().getId(), cnec, side, instant);
    }

    @Override
    public void checkConsistency(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(hvdcRangeAction.getNetworkElement().getId());
        if (!(identifiable instanceof HvdcLine)) {
            throw new OpenRaoException(String.format("Unable to create sensitivity variable for HvdcRangeAction %s, on element %s", hvdcRangeAction.getId(), hvdcRangeAction.getNetworkElement().getId()));
        }
    }
}
