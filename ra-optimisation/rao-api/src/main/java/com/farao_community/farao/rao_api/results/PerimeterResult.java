/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

/**
 * This interface gives a perimeter result. It represents a unique state optimization with full network and range actions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PerimeterResult extends BranchResult, RangeActionResult, ObjectiveFunctionResult, NetworkActionResult {

    PerimeterStatus getStatus();
}
