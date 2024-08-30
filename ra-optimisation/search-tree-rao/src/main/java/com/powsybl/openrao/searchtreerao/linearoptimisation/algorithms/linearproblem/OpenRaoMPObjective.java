/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.modelbuilder.LinearArgument;
import com.google.ortools.modelbuilder.WeightedSumExpression;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPObjective {
    Map<OpenRaoMPVariable, Double> coefficients = new HashMap<>();

    protected OpenRaoMPObjective() {

    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return coefficients.getOrDefault(variable, 0.);
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        coefficients.put(variable, coeff);
    }

    LinearArgument toLinearArgument() {
        int[] indices = new int[coefficients.size()];
        double[] coefs = new double[coefficients.size()];
        int i = 0;
        for (Map.Entry<OpenRaoMPVariable, Double> entry : coefficients.entrySet()) {
            indices[i] = entry.getKey().getMPVariable().getIndex();
            coefs[i] = entry.getValue();
            i++;
        }
        return new WeightedSumExpression(indices, coefs, 0.);
    }
}
