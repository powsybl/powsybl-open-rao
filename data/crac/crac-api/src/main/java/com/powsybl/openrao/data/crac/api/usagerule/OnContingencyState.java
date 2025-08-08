/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;

/**
 * The OnContingencyState UsageRule is defined on a given State. For instance, if a RemedialAction
 * has an OnContingencyState UsageRule with State "curative-co1", this RemedialAction will be
 * available in the State "curative-co1". If the state is "auto", it will be forced.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface OnContingencyState extends UsageRule {

    /**
     * Get the State of the OnContingencyState usage rule
     */
    State getState();

    /**
     * Get the Contingency associated to the state of the OnContingencyState usage rule
     */
    Contingency getContingency();
}
