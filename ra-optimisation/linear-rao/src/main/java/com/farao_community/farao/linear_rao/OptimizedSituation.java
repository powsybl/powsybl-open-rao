/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;

/**
 * The OptimizedSituation is an AbstractSituation in which the RangeAction
 * set-points have been optimized. The LinearRao might go through several
 * OptimizedSituations as its algorithm iterates over several network situations.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class OptimizedSituation extends AbstractSituation {

    /**
     * Constructor
     */
    OptimizedSituation(Network network, String referenceNetworkVariantId, Crac crac) {
        super(network, referenceNetworkVariantId, crac);
        setInitialSituation(false);
    }

    @Override
    String getVariantPrefix() {
        return "postOptimisationResults-";
    }
}
