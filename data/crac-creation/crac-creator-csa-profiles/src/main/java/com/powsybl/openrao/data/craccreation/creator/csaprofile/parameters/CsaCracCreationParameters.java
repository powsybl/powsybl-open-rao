/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;

import java.util.Map;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // swe as default
    private int spsMaxTimeToImplementThresholdInSeconds = 0;
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // SWE CCR as default
    private Map<String, Boolean> usePatlInFinalState = Map.of("REE", false, "REN", true, "RTE", true);
    private Map<String, Integer> craApplicationWindow = Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200);

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
        this.usePatlInFinalState = usePatlInFinalState;
    }

    public void setCraApplicationWindow(Map<String, Integer> craApplicationWindow) {
        this.craApplicationWindow = craApplicationWindow;
    }
}
