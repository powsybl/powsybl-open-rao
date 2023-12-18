/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.rao_api.parameters;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.commons.config.PlatformConfig;

import java.util.*;

import static com.powsybl.open_rao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.open_rao.rao_api.RaoParametersCommons.*;
/**
 * Second preventive parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RaUsageLimitsPerContingencyParameters {
    private static final int DEFAULT_MAX_CURATIVE_RA = Integer.MAX_VALUE;
    private static final int DEFAULT_MAX_CURATIVE_TSO = Integer.MAX_VALUE;
    private static final Map<String, Integer> DEFAULT_MAX_CURATIVE_TOPO_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_CURATIVE_PST_PER_TSO = new HashMap<>();
    private static final Map<String, Integer> DEFAULT_MAX_CURATIVE_RA_PER_TSO = new HashMap<>();
    private int maxCurativeRa = DEFAULT_MAX_CURATIVE_RA;
    private int maxCurativeTso = DEFAULT_MAX_CURATIVE_TSO;
    private Map<String, Integer> maxCurativeTopoPerTso = DEFAULT_MAX_CURATIVE_TOPO_PER_TSO;
    private Map<String, Integer> maxCurativePstPerTso = DEFAULT_MAX_CURATIVE_PST_PER_TSO;
    private Map<String, Integer> maxCurativeRaPerTso = DEFAULT_MAX_CURATIVE_RA_PER_TSO;

    public static RaUsageLimitsPerContingencyParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RaUsageLimitsPerContingencyParameters parameters = new RaUsageLimitsPerContingencyParameters();
        platformConfig.getOptionalModuleConfig(RA_USAGE_LIMITS_PER_CONTINGENCY_SECTION)
                .ifPresent(config -> {
                    parameters.setMaxCurativeRa(config.getIntProperty(MAX_CURATIVE_RA, DEFAULT_MAX_CURATIVE_RA));
                    parameters.setMaxCurativeTso(config.getIntProperty(MAX_CURATIVE_TSO, DEFAULT_MAX_CURATIVE_TSO));
                    parameters.setMaxCurativeTopoPerTso(ParametersUtil.convertListToStringIntMap(config.getStringListProperty(MAX_CURATIVE_TOPO_PER_TSO, ParametersUtil.convertStringIntMapToList(DEFAULT_MAX_CURATIVE_TOPO_PER_TSO))));
                    parameters.setMaxCurativePstPerTso(ParametersUtil.convertListToStringIntMap(config.getStringListProperty(MAX_CURATIVE_PST_PER_TSO, ParametersUtil.convertStringIntMapToList(DEFAULT_MAX_CURATIVE_PST_PER_TSO))));
                    parameters.setMaxCurativeRaPerTso(ParametersUtil.convertListToStringIntMap(config.getStringListProperty(MAX_CURATIVE_RA_PER_TSO, ParametersUtil.convertStringIntMapToList(DEFAULT_MAX_CURATIVE_RA_PER_TSO))));
                });
        return parameters;
    }

    public void setMaxCurativeRa(int maxCurativeRa) {
        if (maxCurativeRa < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of curative RAs is smaller than 0. It will be set to 0 instead.", maxCurativeRa);
            this.maxCurativeRa = 0;
        } else {
            this.maxCurativeRa = maxCurativeRa;
        }
    }

    public void setMaxCurativeTso(int maxCurativeTso) {
        if (maxCurativeTso < 0) {
            BUSINESS_WARNS.warn("The value {} provided for max number of curative TSOs is smaller than 0. It will be set to 0 instead.", maxCurativeTso);
            this.maxCurativeTso = 0;
        } else {
            this.maxCurativeTso = maxCurativeTso;
        }
    }

    public void setMaxCurativeTopoPerTso(Map<String, Integer> maxCurativeTopoPerTso) {
        if (Objects.isNull(maxCurativeTopoPerTso)) {
            this.maxCurativeTopoPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(this.maxCurativeRaPerTso, maxCurativeTopoPerTso, this.maxCurativePstPerTso);
            this.maxCurativeTopoPerTso = maxCurativeTopoPerTso;
        }
    }

    public void setMaxCurativePstPerTso(Map<String, Integer> maxCurativePstPerTso) {
        if (Objects.isNull(maxCurativePstPerTso)) {
            this.maxCurativePstPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(this.maxCurativeRaPerTso, this.maxCurativeTopoPerTso, maxCurativePstPerTso);
            this.maxCurativePstPerTso = maxCurativePstPerTso;
        }
    }

    public void setMaxCurativeRaPerTso(Map<String, Integer> maxCurativeRaPerTso) {
        if (Objects.isNull(maxCurativeRaPerTso)) {
            this.maxCurativeRaPerTso = new HashMap<>();
        } else {
            crossCheckMaxCraPerTsoParameters(maxCurativeRaPerTso, this.maxCurativeTopoPerTso, this.maxCurativePstPerTso);
            this.maxCurativeRaPerTso = maxCurativeRaPerTso;
        }
    }

    public int getMaxCurativeRa() {
        return maxCurativeRa;
    }

    public int getMaxCurativeTso() {
        return maxCurativeTso;
    }

    public Map<String, Integer> getMaxCurativeTopoPerTso() {
        return maxCurativeTopoPerTso;
    }

    public Map<String, Integer> getMaxCurativePstPerTso() {
        return maxCurativePstPerTso;
    }

    public Map<String, Integer> getMaxCurativeRaPerTso() {
        return maxCurativeRaPerTso;
    }

    private static void crossCheckMaxCraPerTsoParameters(Map<String, Integer> maxCurativeRaPerTso, Map<String, Integer> maxCurativeTopoPerTso, Map<String, Integer> maxCurativePstPerTso) {
        Set<String> tsos = new HashSet<>();
        tsos.addAll(maxCurativeRaPerTso.keySet());
        tsos.addAll(maxCurativeTopoPerTso.keySet());
        tsos.addAll(maxCurativePstPerTso.keySet());
        tsos.forEach(tso -> {
            if (maxCurativeTopoPerTso.containsKey(tso)
                    && maxCurativeRaPerTso.getOrDefault(tso, 1000) < maxCurativeTopoPerTso.get(tso)) {
                throw new FaraoException(String.format("TSO %s has a maximum number of allowed CRAs smaller than the number of allowed topological CRAs. This is not supported.", tso));
            }
            if (maxCurativePstPerTso.containsKey(tso)
                    && maxCurativeRaPerTso.getOrDefault(tso, 1000) < maxCurativePstPerTso.get(tso)) {
                throw new FaraoException(String.format("TSO %s has a maximum number of allowed CRAs smaller than the number of allowed PST CRAs. This is not supported.", tso));
            }
        });
    }

}
