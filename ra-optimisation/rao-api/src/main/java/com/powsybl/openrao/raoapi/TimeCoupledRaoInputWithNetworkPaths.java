/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.timecouplingconstraints.TimeCouplingConstraints;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class TimeCoupledRaoInputWithNetworkPaths {
    private final TemporalData<RaoInputWithNetworkPaths> raoInputs;
    private final Set<OffsetDateTime> timestampsToRun;
    private final TimeCouplingConstraints timeCouplingConstraints;

    public TimeCoupledRaoInputWithNetworkPaths(TemporalData<RaoInputWithNetworkPaths> raoInputs, Set<OffsetDateTime> timestampsToRun, TimeCouplingConstraints timeCouplingConstraints) {
        this.raoInputs = raoInputs;
        this.timestampsToRun = new TreeSet<>(timestampsToRun);
        this.timeCouplingConstraints = timeCouplingConstraints;
        checkTimestampsToRun();
    }

    public TimeCoupledRaoInputWithNetworkPaths(TemporalData<RaoInputWithNetworkPaths> raoInputs, TimeCouplingConstraints timeCouplingConstraints) {
        this(raoInputs, new HashSet<>(raoInputs.getTimestamps()), timeCouplingConstraints);
    }

    public TemporalData<RaoInputWithNetworkPaths> getRaoInputs() {
        return raoInputs;
    }

    public Set<OffsetDateTime> getTimestampsToRun() {
        return timestampsToRun;
    }

    public TimeCouplingConstraints getTimeCouplingConstraints() {
        return timeCouplingConstraints;
    }

    private void checkTimestampsToRun() {
        Set<String> invalidTimestampsToRun = timestampsToRun.stream().filter(timestamp -> !raoInputs.getTimestamps().contains(timestamp)).map(OffsetDateTime::toString).collect(Collectors.toSet());
        if (!invalidTimestampsToRun.isEmpty()) {
            throw new OpenRaoException("Timestamp(s) '" + String.join("', '", invalidTimestampsToRun) + "' are not defined in the inputs.");
        }
    }
}
