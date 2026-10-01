/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOUTRE_PARAMETERS;

/**
 * Parameters specific to the LOUTRe (linear RAO) provider.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LoutreParameters extends AbstractExtension<RaoParameters> {
    static final List<String> DEFAULT_FORCED_NETWORK_ACTIONS = List.of();

    private List<String> forcedNetworkActions = new ArrayList<>(DEFAULT_FORCED_NETWORK_ACTIONS);

    @Override
    public String getName() {
        return LOUTRE_PARAMETERS;
    }

    /**
     * @return the ids of the preventive network actions to apply before the linear RAO starts
     */
    public List<String> getForcedNetworkActions() {
        return forcedNetworkActions;
    }

    public void setForcedNetworkActions(List<String> forcedNetworkActions) {
        this.forcedNetworkActions = new ArrayList<>(Objects.requireNonNull(forcedNetworkActions));
    }
}
