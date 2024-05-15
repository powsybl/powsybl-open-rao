/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;

import java.util.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class RaUsageLimits {
    private static final int DEFAULT_MAX_RA = Integer.MAX_VALUE;
    private static final int DEFAULT_MAX_TSO = Integer.MAX_VALUE;
    private static final Map<String, Integer> DEFAULT_MAX_TOPO_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_PST_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_RA_PER_TSO = new HashMap<>();
    private int maxRa = DEFAULT_MAX_RA;
    private int maxTso = DEFAULT_MAX_TSO;
    private final Set<String> maxTsoExclusion = new HashSet<>();
    private Map<String, Integer> maxTopoPerTso = DEFAULT_MAX_TOPO_PER_TSO;
    private Map<String, Integer> maxPstPerTso = DEFAULT_MAX_PST_PER_TSO;
    private Map<String, Integer> maxRaPerTso = DEFAULT_MAX_RA_PER_TSO;

    public void setMaxRa(int maxRa) {
        setMaxRa(maxRa, ReportNode.NO_OP);
    }

    public void setMaxRa(int maxRa, ReportNode reportNode) {
        if (maxRa < 0) {
            CracApiReports.reportRaUsageLimitsNegativeMaxRa(reportNode, maxRa);
            this.maxRa = 0;
        } else {
            this.maxRa = maxRa;
        }
    }

    public void setMaxTso(int maxTso) {
        setMaxTso(maxTso, ReportNode.NO_OP);
    }

    public void setMaxTso(int maxTso, ReportNode reportNode) {
        if (maxTso < 0) {
            CracApiReports.reportRaUsageLimitsNegativeMaxTso(reportNode, maxTso);
            this.maxTso = 0;
        } else {
            this.maxTso = maxTso;
        }
    }

    public void setMaxTopoPerTso(Map<String, Integer> maxTopoPerTso) {
        setMaxTopoPerTso(maxTopoPerTso, ReportNode.NO_OP);
    }

    public void setMaxTopoPerTso(Map<String, Integer> maxTopoPerTso, ReportNode reportNode) {
        if (Objects.isNull(maxTopoPerTso)) {
            this.maxTopoPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxTopoPerTso = replaceNegativeValues(maxTopoPerTso, reportNode);
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, updatedMaxTopoPerTso, this.maxPstPerTso);
            this.maxTopoPerTso = updatedMaxTopoPerTso;
        }
    }

    public void setMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
        setMaxPstPerTso(maxPstPerTso, ReportNode.NO_OP);
    }

    public void setMaxPstPerTso(Map<String, Integer> maxPstPerTso, ReportNode reportNode) {
        if (Objects.isNull(maxPstPerTso)) {
            this.maxPstPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxPstPerTso = replaceNegativeValues(maxPstPerTso, reportNode);
            crossCheckMaxCraPerTsoParameters(this.maxRaPerTso, this.maxTopoPerTso, updatedMaxPstPerTso);
            this.maxPstPerTso = updatedMaxPstPerTso;
        }
    }

    public void setMaxRaPerTso(Map<String, Integer> maxRaPerTso) {
        setMaxRaPerTso(maxRaPerTso, ReportNode.NO_OP);
    }

    public void setMaxRaPerTso(Map<String, Integer> maxRaPerTso, ReportNode reportNode) {
        if (Objects.isNull(maxRaPerTso)) {
            this.maxRaPerTso = new HashMap<>();
        } else {
            Map<String, Integer> updatedMaxRaPerTso = replaceNegativeValues(maxRaPerTso, reportNode);
            crossCheckMaxCraPerTsoParameters(updatedMaxRaPerTso, this.maxTopoPerTso, this.maxPstPerTso);
            this.maxRaPerTso = updatedMaxRaPerTso;
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

    public Set<String> getMaxTsoExclusion() {
        return maxTsoExclusion;
    }

    private Map<String, Integer> replaceNegativeValues(Map<String, Integer> limitsPerTso, ReportNode reportNode) {
        limitsPerTso.forEach((tso, limit) -> {
            if (limit < 0) {
                CracApiReports.reportRaUsageLimitsNegativeMaxRaForTso(reportNode, limit, tso);
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
