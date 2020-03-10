/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.State;

import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionResultsExtension extends AbstractResultExtension<NetworkAction, NetworkActionResult> {

    public NetworkActionResult addVariant(String newVariantId, Set<State> states) {
        return addVariant(newVariantId, new NetworkActionResult(states));
    }

    @Override
    public String getName() {
        return "NetworkActionResultsExtension";
    }
}
