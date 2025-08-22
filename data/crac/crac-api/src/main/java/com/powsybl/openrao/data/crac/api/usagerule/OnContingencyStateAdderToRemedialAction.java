/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;

/**
 * Adds a OnContingencyState usage rule to a RemedialAction
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnContingencyStateAdderToRemedialAction<T extends RemedialAction<T>> {

    OnContingencyStateAdderToRemedialAction<T> withState(State state);

    OnContingencyStateAdderToRemedialAction<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
