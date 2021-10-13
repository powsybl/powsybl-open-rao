/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.std_creation_context;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public interface HvdcRangeActionCreationContext extends RemedialActionCreationContext {
    /**
     * Know if the HVDC  is inverted in the network
     */
    boolean isInverted();

    /**
     * Get the ID of the element as present in the native CRAC file
     */
    String getNativeNetworkElementId();
}
