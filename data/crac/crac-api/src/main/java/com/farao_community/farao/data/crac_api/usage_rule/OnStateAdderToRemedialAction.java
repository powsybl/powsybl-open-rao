/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.usage_rule;

import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;

/**
 * Adds a OnState usage rule to a RemedialAction
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnStateAdderToRemedialAction<T extends RemedialAction<T>> {

    OnStateAdderToRemedialAction<T> withState(State state);

    OnStateAdderToRemedialAction<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
