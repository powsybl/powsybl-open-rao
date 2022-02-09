/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPVariable extends MPVariable {
    private final int numberOfBitsToRoundOff;

    protected FaraoMPVariable(long cPtr, boolean cMemoryOwn, int numberOfBitsToRoundOff) {
        super(cPtr, cMemoryOwn);
        this.numberOfBitsToRoundOff = numberOfBitsToRoundOff;
    }

    @Override
    public void setLb(double lb) {
        super.setLb(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff));
    }

    @Override
    public void setUb(double ub) {
        super.setUb(RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }

    @Override
    public void setBounds(double lb, double ub) {
        super.setBounds(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff), RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }
}
