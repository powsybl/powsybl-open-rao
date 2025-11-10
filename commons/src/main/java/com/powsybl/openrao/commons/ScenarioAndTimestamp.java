/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ScenarioAndTimestamp implements Comparable<ScenarioAndTimestamp> {
    private final String scenario;
    private final OffsetDateTime timestamp;

    public ScenarioAndTimestamp(String scenario, OffsetDateTime timestamp) {
        this.scenario = scenario;
        this.timestamp = timestamp;
    }

    public String getScenario() {
        return scenario;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return scenario.equals(((ScenarioAndTimestamp) o).scenario) &&
            timestamp.equals(((ScenarioAndTimestamp) o).timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, timestamp);
    }

    @Override
    public int compareTo(ScenarioAndTimestamp other) {
        return 10 * scenario.compareTo(other.scenario) + timestamp.compareTo(other.timestamp);
    }
}
