/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimits {
    private static final int DEFAULT_MAX_RA = Integer.MAX_VALUE;
    private static final int DEFAULT_MAX_TSO = Integer.MAX_VALUE;
    private static final Map<String, Integer> DEFAULT_MAX_TOPO_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_PST_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_RA_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_ELEMENTARY_ACTIONS_PER_TSO = new HashMap<>();
    private int maxRa = DEFAULT_MAX_RA;
    private int maxTso = DEFAULT_MAX_TSO;
    private final Set<String> maxTsoExclusion = new HashSet<>();
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

    public void setMaxTso(int maxTso) {
        if (maxTso < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of TSOs is smaller than 0. It will be set to 0 instead.", maxTso);
            this.maxTso = 0;
        } else {
            this.maxTso = maxTso;
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

    public int getMaxTso() {
        return maxTso;
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

    public Set<String> getMaxTsoExclusion() {
        return maxTsoExclusion;
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

    public void addTsoToExclude(String tso) {
        maxTsoExclusion.add(tso);
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
            && raUsageLimits.maxTso == this.maxTso
            && raUsageLimits.maxRaPerTso.equals(this.maxRaPerTso)
            && raUsageLimits.maxPstPerTso.equals(this.maxPstPerTso)
            && raUsageLimits.maxTopoPerTso.equals(this.maxTopoPerTso);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
