/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.parameters;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SweNcCracCreationParameters extends NcCracCreationParameters {
    private Set<String> tsosWhichDoNotUsePatlInFinalState = Set.of();

    @Override
    public String getName() {
        return "SweNcCracCreatorParameters";
    }

    public Set<String> getTsosWhichDoNotUsePatlInFinalState() {
        return tsosWhichDoNotUsePatlInFinalState;
    }

    public void setTsosWhichDoNotUsePatlInFinalState(Set<String> tsosWhichDoNotUsePatlInFinalState) {
        this.tsosWhichDoNotUsePatlInFinalState = new HashSet<>(tsosWhichDoNotUsePatlInFinalState);
    }
}
