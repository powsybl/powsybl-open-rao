/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_commons.RaoUtil;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPObjective extends MPObjective {
    private final int numberOfBitsToRoundOff;
    List<MPVariable> variables = new ArrayList<>();

    protected FaraoMPObjective(long cPtr, boolean cMemoryOwn, int numberOfBitsToRoundOff) {
        super(cPtr, cMemoryOwn);
        this.numberOfBitsToRoundOff = numberOfBitsToRoundOff;
    }

    @Override
    public void setCoefficient(MPVariable variable, double coeff) {
        variables.add(variable);
        super.setCoefficient(variable, RaoUtil.roundDouble(coeff, numberOfBitsToRoundOff));
    }

    public List<MPVariable> getVariables() {
        return variables;
    }
}
