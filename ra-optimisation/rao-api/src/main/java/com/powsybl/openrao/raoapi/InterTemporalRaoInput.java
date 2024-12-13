/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.TemporalData;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalRaoInput {
    private final TemporalData<RaoInput> raoInputs;
    private final Set<OffsetDateTime> timestampsToRun;

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs, Set<OffsetDateTime> timestampsToRun) {
        this.raoInputs = raoInputs;
        this.timestampsToRun = timestampsToRun;
    }

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs) {
        this(raoInputs, raoInputs.getTimestamps().stream().collect(Collectors.toSet()));
    }

    public TemporalData<RaoInput> getRaoInputs() {
        return raoInputs;
    }

    public Set<OffsetDateTime> getTimestampsToRun() {
        return timestampsToRun;
    }

}
