/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface PstRangeActionCreationContext extends ElementaryCreationContext {
    /**
     * Know if the transformer is inverted in the network
     */
    boolean isInverted();

    /**
     * Get the ID of the element as present in the native CRAC file
     */
    String getNativeNetworkElementId();
}
