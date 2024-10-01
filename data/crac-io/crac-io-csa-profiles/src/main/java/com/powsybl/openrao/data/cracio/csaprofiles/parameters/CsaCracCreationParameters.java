/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // swe as default
    private int spsMaxTimeToImplementThresholdInSeconds = 0;
    private Set<String> tsosWhichDoNotUsePatlInFinalState = Set.of();
    private Map<String, Integer> curativeBatchPostOutageTime = Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200);
    private Set<Border> borders = Set.of();

    @Override
    public String getName() {
        return "CsaCracCreatorParameters";
    }

    public String getCapacityCalculationRegionEicCode() {
        return capacityCalculationRegionEicCode;
    }

    public Set<String> getTsosWhichDoNotUsePatlInFinalState() {
        return tsosWhichDoNotUsePatlInFinalState;
    }

    public Map<String, Integer> getCurativeBatchPostOutageTime() {
        return curativeBatchPostOutageTime;
    }

    public Set<Border> getBorders() {
        return borders;
    }

    public void setCapacityCalculationRegionEicCode(String capacityCalculationRegionEicCode) {
        this.capacityCalculationRegionEicCode = capacityCalculationRegionEicCode;
    }

    public int getSpsMaxTimeToImplementThresholdInSeconds() {
        return spsMaxTimeToImplementThresholdInSeconds;
    }

    public void setSpsMaxTimeToImplementThresholdInSeconds(int spsMaxTimeToImplementThresholdInSeconds) {
        this.spsMaxTimeToImplementThresholdInSeconds = spsMaxTimeToImplementThresholdInSeconds;
    }

    public void setTsosWhichDoNotUsePatlInFinalState(Set<String> tsosWhichDoNotUsePatlInFinalState) {
        this.tsosWhichDoNotUsePatlInFinalState = new HashSet<>(tsosWhichDoNotUsePatlInFinalState);
    }

    public void setCurativeBatchPostOutageTime(Map<String, Integer> curativeBatchPostOutageTime) {
        this.curativeBatchPostOutageTime = new HashMap<>(curativeBatchPostOutageTime);
    }

    public void setBorders(Set<Border> borders) {
        this.borders = new HashSet<>(borders);
    }
}
