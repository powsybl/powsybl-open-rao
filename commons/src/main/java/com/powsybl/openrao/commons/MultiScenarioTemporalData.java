/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MultiScenarioTemporalData<T> {
    private Map<ScenarioAndTimestamp, T> data;

    public MultiScenarioTemporalData() {
        this(new TreeMap<>());
    }

    public MultiScenarioTemporalData(Map<ScenarioAndTimestamp, ? extends T> data) {
        this.data = new TreeMap<>(data);
    }

    public Map<ScenarioAndTimestamp, T> getAllData() {
        return new TreeMap<>(data);
    }

    public Optional<T> get(String scenario, OffsetDateTime timestamp) {
        return Optional.ofNullable(getAllData().get(new ScenarioAndTimestamp(scenario, timestamp)));
    }

    public List<OffsetDateTime> getTimestamps() {
        return getAllData().keySet().stream().map(ScenarioAndTimestamp::getTimestamp).distinct().sorted().toList();
    }

    public List<String> getScenarios() {
        return getAllData().keySet().stream().map(ScenarioAndTimestamp::getScenario).distinct().sorted().toList();
    }

    public void put(String scenario, OffsetDateTime timestamp, T data) {
        this.data.put(new ScenarioAndTimestamp(scenario, timestamp), data);
    }

    public <U> MultiScenarioTemporalData<U> map(Function<T, U> function) {
        return new MultiScenarioTemporalData<>(data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> function.apply(entry.getValue()))));
    }
}
