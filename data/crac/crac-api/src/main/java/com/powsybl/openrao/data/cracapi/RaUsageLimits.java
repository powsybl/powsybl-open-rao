package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public class RaUsageLimits {
    private static final int DEFAULT_MAX_RA = Integer.MAX_VALUE;
    private static final int DEFAULT_MAX_TSO = Integer.MAX_VALUE;
    private static final Map<String, Integer> DEFAULT_MAX_TOPO_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_PST_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_RA_PER_TSO = new HashMap<>();
    private int maxRa = DEFAULT_MAX_RA;
    private int maxTso = DEFAULT_MAX_TSO;
    private Map<String, Integer> maxTopoPerTso = DEFAULT_MAX_TOPO_PER_TSO;
    private Map<String, Integer> maxPstPerTso = DEFAULT_MAX_PST_PER_TSO;
    private Map<String, Integer> maxRaPerTso = DEFAULT_MAX_RA_PER_TSO;

    public void setMaxRa(int maxRa) {
        if (maxRa < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of  RAs is smaller than 0. It will be set to 0 instead.", maxRa);
            this.maxRa = 0;
        } else {
            this.maxRa = maxRa;
        }
    }

    public void setMaxTso(int maxTso) {
        if (maxTso < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of  TSOs is smaller than 0. It will be set to 0 instead.", maxTso);
            this.maxTso = 0;
        } else {
            this.maxTso = maxTso;
        }
    }

    public void setMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        if (Objects.isNull(maxTopoPerTso)) {
            this.maxTopoPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, maxTopoPerTso, this.maxPstPerTso);
            this.maxTopoPerTso = maxTopoPerTso;
        }
    }

    public void setMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        if (Objects.isNull(maxPstPerTso)) {
            this.maxPstPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, this.maxTopoPerTso, maxPstPerTso);
            this.maxPstPerTso = maxPstPerTso;
        }
    }

    public void setMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        if (Objects.isNull(maxRaPerTso)) {
            this.maxRaPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(maxRaPerTso, this.maxTopoPerTso, this.maxPstPerTso);
            this.maxRaPerTso = maxRaPerTso;
        }
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

    private static void crossCheckMaxCraPerTsoParameters(Map<String, Integer> maxRaPerTso, Map<String, Integer> maxTopoPerTso, Map<String, Integer> maxPstPerTso) {
        Set<String> tsos = new HashSet<>();
        tsos.addAll(maxRaPerTso.keySet());
        tsos.addAll(maxTopoPerTso.keySet());
        tsos.addAll(maxPstPerTso.keySet());
        tsos.forEach(tso -> {
            if (maxTopoPerTso.containsKey(tso)
                && maxRaPerTso.getOrDefault(tso, 1000) < maxTopoPerTso.get(tso)) {
                throw new OpenRaoException(String.format("TSO %s has a maximum number of allowed RAs smaller than the number of allowed topological RAs. This is not supported.", tso));
            }
            if (maxPstPerTso.containsKey(tso)
                && maxRaPerTso.getOrDefault(tso, 1000) < maxPstPerTso.get(tso)) {
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
        return raUsageLimits.maxRa == this.getMaxRa()
            && raUsageLimits.maxTso == this.getMaxTso()
            && raUsageLimits.maxRaPerTso.equals(this.getMaxRaPerTso())
            && raUsageLimits.maxPstPerTso.equals(this.getMaxPstPerTso())
            && raUsageLimits.maxTopoPerTso.equals(this.getMaxTopoPerTso());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
