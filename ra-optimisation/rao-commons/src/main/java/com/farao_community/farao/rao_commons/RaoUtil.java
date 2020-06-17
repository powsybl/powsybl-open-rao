/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private RaoUtil() { }

    public static RaoData initRaoData(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        network.getVariantManager().setWorkingVariant(variantId);
        RaoInput.cleanCrac(crac, network);
        RaoInput.synchronize(crac, network);
        RaoData raoData = new RaoData(network, crac);

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowComputation.checkDataConsistency(raoData);
            LoopFlowComputation.computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(raoData);
        }

        return raoData;
    }
}
