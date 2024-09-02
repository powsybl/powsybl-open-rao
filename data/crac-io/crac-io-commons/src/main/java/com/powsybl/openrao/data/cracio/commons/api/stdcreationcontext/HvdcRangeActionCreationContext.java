/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public interface HvdcRangeActionCreationContext extends ElementaryCreationContext {
    /**
     * Know if the HVDC  is inverted in the network
     */
    boolean isInverted();

    /**
     * Get the ID of the element as present in the native CRAC file
     */
    String getNativeNetworkElementId();
}
