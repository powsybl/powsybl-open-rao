/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.generatorconstraints.GeneratorConstraints;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalRaoInput {
    private final TemporalData<RaoInput> raoInputs;
    private final Set<OffsetDateTime> timestampsToRun;
    private final Set<GeneratorConstraints> generatorConstraints;

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs, Set<OffsetDateTime> timestampsToRun, Set<GeneratorConstraints> powerGradients) {
        this.raoInputs = raoInputs;
        this.timestampsToRun = timestampsToRun;
        this.generatorConstraints = powerGradients;
        checkTimestampsToRun();
    }

    public InterTemporalRaoInput(TemporalData<RaoInput> raoInputs, Set<GeneratorConstraints> generatorConstraints) {
        this(raoInputs, new HashSet<>(raoInputs.getTimestamps()), generatorConstraints);
    }

    public TemporalData<RaoInput> getRaoInputs() {
        return raoInputs;
    }

    public Set<OffsetDateTime> getTimestampsToRun() {
        return timestampsToRun;
    }

    public Set<GeneratorConstraints> getGeneratorConstraints() {
        return generatorConstraints;
    }

    private void checkTimestampsToRun() {
        Set<String> invalidTimestampsToRun = timestampsToRun.stream().filter(timestamp -> !raoInputs.getTimestamps().contains(timestamp)).map(OffsetDateTime::toString).collect(Collectors.toSet());
        if (!invalidTimestampsToRun.isEmpty()) {
            throw new OpenRaoException("Timestamp(s) '" + String.join("', '", invalidTimestampsToRun) + "' are not defined in the inputs.");
        }
    }
}
