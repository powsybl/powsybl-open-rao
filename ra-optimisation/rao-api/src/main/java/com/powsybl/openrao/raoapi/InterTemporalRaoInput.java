/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalRaoInput {
    private final TemporalData<RaoInput> raoInputs;
    private final Set<OffsetDateTime> timestampsToRun;
    private final Set<PowerGradient> powerGradients;

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs, Set<OffsetDateTime> timestampsToRun, Set<PowerGradient> powerGradients) {
        this.raoInputs = raoInputs;
        this.timestampsToRun = timestampsToRun;
        this.powerGradients = powerGradients;
    }

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs, Set<PowerGradient> powerGradients) {
        this(raoInputs, new HashSet<>(raoInputs.getTimestamps()), powerGradients);
    }

    public TemporalData<RaoInput> getRaoInputs() {
        return raoInputs;
    }

    public Set<OffsetDateTime> getTimestampsToRun() {
        return timestampsToRun;
    }

    public Set<PowerGradient> getPowerGradientConstraints() {
        return powerGradients;
    }
}
