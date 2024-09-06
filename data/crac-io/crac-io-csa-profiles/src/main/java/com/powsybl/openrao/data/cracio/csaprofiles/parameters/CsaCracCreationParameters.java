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
    private static final String REE = "REE";
    private static final String REN = "REN";
    private static final String RTE = "RTE";
    private static final String ES_FR = "ES-FR";
    private static final String ES_PT = "ES-PT";
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // swe as default
    private int spsMaxTimeToImplementThresholdInSeconds = 0;
    private Map<String, Boolean> usePatlInFinalState = Map.of(REE, false, REN, true, RTE, true);
    private Map<String, Integer> craApplicationWindow = Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200);
    private Set<Border> borders = Set.of(new Border(ES_FR, "10YDOM--ES-FR--D", RTE), new Border(ES_PT, "10YDOM--ES-PT--T", REN));

    @Override
    public String getName() {
        return "CsaCracCreatorParameters";
    }

    public String getCapacityCalculationRegionEicCode() {
        return capacityCalculationRegionEicCode;
    }

    public Map<String, Boolean> getUsePatlInFinalState() {
        return usePatlInFinalState;
    }

    public Map<String, Integer> getCraApplicationWindow() {
        return craApplicationWindow;
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

    public void setUsePatlInFinalState(Map<String, Boolean> usePatlInFinalState) {
        this.usePatlInFinalState = new HashMap<>(usePatlInFinalState);
    }

    public void setCraApplicationWindow(Map<String, Integer> craApplicationWindow) {
        this.craApplicationWindow = new HashMap<>(craApplicationWindow);
    }

    public void setBorders(Set<Border> borders) {
        this.borders = new HashSet<>(borders);
    }
}
