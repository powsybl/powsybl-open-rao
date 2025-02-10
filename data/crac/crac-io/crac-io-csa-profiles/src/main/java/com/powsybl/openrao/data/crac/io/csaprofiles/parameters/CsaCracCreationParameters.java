/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.csaprofiles.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // swe as default
    private Set<String> tsosWhichDoNotUsePatlInFinalState = Set.of();
    private int autoInstantApplicationTime = 0;
    private Map<String, Integer> curativeInstants = Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200);
    private Set<Border> borders = Set.of();
    private OffsetDateTime timestamp;

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

    public int getAutoInstantApplicationTime() {
        return autoInstantApplicationTime;
    }

    public Map<String, Integer> getCurativeInstants() {
        return curativeInstants;
    }

    public Set<Border> getBorders() {
        return borders;
    }

    public void setCapacityCalculationRegionEicCode(String capacityCalculationRegionEicCode) {
        this.capacityCalculationRegionEicCode = capacityCalculationRegionEicCode;
    }

    public void setAutoInstantApplicationTime(int autoInstantApplicationTime) {
        this.autoInstantApplicationTime = autoInstantApplicationTime;
    }

    public void setTsosWhichDoNotUsePatlInFinalState(Set<String> tsosWhichDoNotUsePatlInFinalState) {
        this.tsosWhichDoNotUsePatlInFinalState = new HashSet<>(tsosWhichDoNotUsePatlInFinalState);
    }

    public void setCurativeInstants(Map<String, Integer> curativeInstants) {
        this.curativeInstants = new HashMap<>(curativeInstants);
    }

    public void setBorders(Set<Border> borders) {
        this.borders = new HashSet<>(borders);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
