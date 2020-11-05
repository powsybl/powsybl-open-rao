/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.converters;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class NetworkUtil {
    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private NetworkUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static double pseudoTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    static double pseudoP0(Load load) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(load.getP0()));
    }

    static boolean isCorrectGenerator(Generator generator) {
        return generator.getTerminal().isConnected() &&
                generator.getTerminal().getBusView().getBus() != null &&
                generator.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    static boolean isCorrectLoad(Load load) {
        return load.getTerminal().isConnected() &&
                load.getTerminal().getBusView().getBus() != null &&
                load.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }
}
