/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface InjectionRangeActionCreationContext extends ElementaryCreationContext {

    /**
     * For each injection of the range action in the native CRAC file, get the ID of the equivalent element
     * found in the PowSyBl network
     */
    Map<String, String> getNativeNetworkElementIds();
}
