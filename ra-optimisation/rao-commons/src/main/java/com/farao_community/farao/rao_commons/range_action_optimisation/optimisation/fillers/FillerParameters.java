/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.fillers;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FillerParameters {

    static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    static final double DEFAULT_PST_PENALTY_COST = 0.01;

    private double pstPenaltyCost;
    private double pstSensitivityThreshold;

    public FillerParameters() {
        this.pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
        this.pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    }

    public FillerParameters(double pstPenaltyCost, double pstSensitivityThreshold) {
        this.pstPenaltyCost = pstPenaltyCost;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public void setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = pstPenaltyCost;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }
}
