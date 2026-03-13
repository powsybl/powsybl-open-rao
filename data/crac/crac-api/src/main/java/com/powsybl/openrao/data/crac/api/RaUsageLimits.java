/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimits {
    private static final int DEFAULT_MAX_RA = Integer.MAX_VALUE;
    private static final Map<String, Integer> DEFAULT_MAX_TOPO_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_PST_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_RA_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_ELEMENTARY_ACTIONS_PER_TSO = new HashMap<>();
    private int maxRa = DEFAULT_MAX_RA;
    private Map<String, Integer> maxTopoPerTso = DEFAULT_MAX_TOPO_PER_TSO;
    private Map<String, Integer> maxPstPerTso = DEFAULT_MAX_PST_PER_TSO;
    private Map<String, Integer> maxRaPerTso = DEFAULT_MAX_RA_PER_TSO;
    private Map<String, Integer> maxElementaryActionsPerTso = DEFAULT_MAX_ELEMENTARY_ACTIONS_PER_TSO;

    public void setMaxRa(int maxRa) {
        if (maxRa < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of RAs is smaller than 0. It will be set to 0 instead.", maxRa);
            this.maxRa = 0;
        } else {
            this.maxRa = maxRa;
        }
    }

    public void setMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        if (Objects.isNull(maxTopoPerTso)) {
            this.maxTopoPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxTopoPerTso = replaceNegativeValues(maxTopoPerTso);
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, updatedMaxTopoPerTso, this.maxPstPerTso);
            this.maxTopoPerTso = updatedMaxTopoPerTso;
        }
    }

    public void setMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        if (Objects.isNull(maxPstPerTso)) {
            this.maxPstPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxPstPerTso = replaceNegativeValues(maxPstPerTso);
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, this.maxTopoPerTso, updatedMaxPstPerTso);
            this.maxPstPerTso = updatedMaxPstPerTso;
        }
    }

    public void setMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        if (Objects.isNull(maxRaPerTso)) {
            this.maxRaPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxRaPerTso = replaceNegativeValues(maxRaPerTso);
            crossCheckMaxCraPerTsoParameters(updatedMaxRaPerTso, this.maxTopoPerTso, this.maxPstPerTso);
            this.maxRaPerTso = updatedMaxRaPerTso;
        }
    }

    public void setMaxElementaryActionsPerTso(Map<String, Integer> maxElementaryActionsPerTso) {
        this.maxElementaryActionsPerTso = Objects.isNull(maxElementaryActionsPerTso) ? new HashMap<>() : replaceNegativeValues(maxElementaryActionsPerTso);
    }

    public int getMaxRa() {
        return maxRa;
    }

    public Map<String, Integer> getMaxTopoPerTso() {
        return maxTopoPerTso;
    }

    public Map<String, Integer> getMaxPstPerTso() {
        return maxPstPerTso;
    }

    public Map<String, Integer> getMaxRaPerTso() {
        return maxRaPerTso;
    }

    public Map<String, Integer> getMaxElementaryActionsPerTso() {
        return maxElementaryActionsPerTso;
    }

    private Map<String, Integer> replaceNegativeValues(Map<String, Integer> limitsPerTso) {
        limitsPerTso.forEach((tso, limit) -> {
            if (limit < 0) {
                BUSINESS_WARNS.warn("The value {} provided for max number of RAs for TSO {} is smaller than 0. It will be set to 0 instead.", limit, tso);
                limitsPerTso.put(tso, 0);
            }
        });
        return limitsPerTso;
    }

    private static void crossCheckMaxCraPerTsoParameters(Map<String, Integer> maxRaPerTso, Map<String, Integer> maxTopoPerTso, Map<String, Integer> maxPstPerTso) {
        Set<String> tsos = new HashSet<>();
        tsos.addAll(maxRaPerTso.keySet());
        tsos.addAll(maxTopoPerTso.keySet());
        tsos.addAll(maxPstPerTso.keySet());
        tsos.forEach(tso -> {
            if (maxTopoPerTso.containsKey(tso)
                && maxRaPerTso.getOrDefault(tso, DEFAULT_MAX_RA) < maxTopoPerTso.get(tso)) {
                throw new OpenRaoException(String.format("TSO %s has a maximum number of allowed RAs smaller than the number of allowed topological RAs. This is not supported.", tso));
            }
            if (maxPstPerTso.containsKey(tso)
                && maxRaPerTso.getOrDefault(tso, DEFAULT_MAX_RA) < maxPstPerTso.get(tso)) {
                throw new OpenRaoException(String.format("TSO %s has a maximum number of allowed RAs smaller than the number of allowed PST RAs. This is not supported.", tso));
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RaUsageLimits raUsageLimits = (RaUsageLimits) o;
        return raUsageLimits.maxRa == this.maxRa
            && raUsageLimits.maxRaPerTso.equals(this.maxRaPerTso)
            && raUsageLimits.maxPstPerTso.equals(this.maxPstPerTso)
            && raUsageLimits.maxTopoPerTso.equals(this.maxTopoPerTso);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // The deserializer is used in crac deserialization as well as crac creation parameters
    public static Pair<String, RaUsageLimits> deserializeRaUsageLimits(JsonParser jsonParser, Optional<Integer> cracPrimaryVersion, Optional<Integer> cracSubVersion) throws IOException {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        String instant = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case INSTANT:
                    jsonParser.nextToken();
                    instant = jsonParser.getValueAsString();
                    break;
                case MAX_RA:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRa(jsonParser.getIntValue());
                    break;
                case MAX_TSO:
                    if (cracPrimaryVersion.isPresent() && cracSubVersion.isPresent()
                        && (cracPrimaryVersion.get() > 2 || cracPrimaryVersion.get() == 2 && cracSubVersion.get() > 11)) {
                        throw new OpenRaoException("The max-tso limit can no longer be defined since CRAC version 2.8");
                    } else {
                        jsonParser.nextToken();
                        TECHNICAL_LOGS.warn("The max-tso limit can no longer be defined and will be ignored. ");
                        break;
                    }
                case MAX_TOPO_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxTopoPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case MAX_PST_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxPstPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case MAX_RA_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxRaPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case MAX_ELEMENTARY_ACTIONS_PER_TSO:
                    jsonParser.nextToken();
                    raUsageLimits.setMaxElementaryActionsPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                default:
                    throw new OpenRaoException(String.format(
                        "Cannot deserialize ra-usage-limits-per-instant parameters: unexpected field in %s (%s)",
                        RA_USAGE_LIMITS_PER_INSTANT,
                        jsonParser.currentName()
                    ));
            }
        }
        return Pair.of(instant, raUsageLimits);
    }

    private static Map<String, Integer> readStringToPositiveIntMap(JsonParser jsonParser) throws IOException {
        HashMap<String, Integer> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof Integer)) {
                throw new OpenRaoException("Unexpected key or value type in a Map<String, Integer> parameter!");
            }
            if ((int) o2 < 0) {
                throw new OpenRaoException("Unexpected negative integer!");
            }
        });
        return map;
    }
}
