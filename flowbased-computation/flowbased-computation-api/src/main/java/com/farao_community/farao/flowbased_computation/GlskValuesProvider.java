/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.List;

/**
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 */
public interface GlskValuesProvider {
    /**
     * @param network network description
     * @return a list of LinearGLSK
     */
    List<LinearGlsk> getGlskValues(Network network);
}
