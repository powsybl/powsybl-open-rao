/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.roda.parameters;

import com.powsybl.action.Action;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Collections;
import java.util.List;

/**
 * RODA specific RAO parameters.
 * For now, only allows forcing actions on the network before running RAO.
 * Could be useful to avoid pre-processing network multiple times (for example when testing topological changes
 * in an outside loop).
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RodaParameters extends AbstractExtension<RaoParameters> {
    public static final String EXTENSION_NAME = "roda-parameters";
    private final List<Action> forcedPreventiveActions;

    public RodaParameters(List<Action> forcedPreventiveActions) {
        this.forcedPreventiveActions = Collections.unmodifiableList(forcedPreventiveActions);
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    public List<Action> getForcedPreventiveActions() {
        return forcedPreventiveActions;
    }
}
