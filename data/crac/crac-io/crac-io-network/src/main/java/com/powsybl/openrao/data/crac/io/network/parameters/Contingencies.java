/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.Country;

import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class Contingencies {
    Optional<Set<Country>> countries = Optional.empty();
    Optional<Double> minV = Optional.empty();
    Optional<Double> maxV = Optional.empty();

    public Optional<Set<Country>> getCountries() {
        return countries;
    }

    public Optional<Double> getMinV() {
        return minV;
    }

    public Optional<Double> getMaxV() {
        return maxV;
    }
}
