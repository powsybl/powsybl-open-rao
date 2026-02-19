/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.action.Action;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.FORCED_ACTIONS_PARAMETERS;

/**
 * Allows forcing actions on network before running RAO.
 * Could be useful to avoid pre-processing network multiple times (for example when testing topological changes
 * in an outside loop).
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ForcedActions extends AbstractExtension<RaoParameters> {
    private List<Action> preventiveActions;

    public ForcedActions() {
        // nothing to do
    }

    @Override
    public String getName() {
        return FORCED_ACTIONS_PARAMETERS;
    }

    public void setPreventiveActions(List<Action> preventiveActions) {
        this.preventiveActions = new ArrayList<>(preventiveActions);
    }

    public List<Action> getPreventiveActions() {
        return new ArrayList<>(preventiveActions);
    }

}
