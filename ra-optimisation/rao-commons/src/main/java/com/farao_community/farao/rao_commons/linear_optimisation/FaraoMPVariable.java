/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_commons.RaoUtil;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPVariable extends MPVariable {
    private final double precision;

    protected FaraoMPVariable(long cPtr, boolean cMemoryOwn, double precision) {
        super(cPtr, cMemoryOwn);
        this.precision = precision;
    }

    @Override
    public void setLb(double lb) {
        super.setLb(RaoUtil.roundDouble(lb, precision));
    }

    @Override
    public void setUb(double ub) {
        super.setUb(RaoUtil.roundDouble(ub, precision));
    }

    @Override
    public void setBounds(double lb, double ub) {
        super.setBounds(RaoUtil.roundDouble(lb, precision), RaoUtil.roundDouble(ub, precision));
    }
}
