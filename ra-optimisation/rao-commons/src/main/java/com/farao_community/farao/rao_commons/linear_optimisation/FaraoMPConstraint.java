/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPConstraint extends MPConstraint {
    double precision;

    protected FaraoMPConstraint(long cPtr, boolean cMemoryOwn, double precision) {
        super(cPtr, cMemoryOwn);
        this.precision = precision;
    }

    @Override
    public void setCoefficient(MPVariable var, double coeff) {
        super.setCoefficient(var, Math.round(coeff * precision) / precision);
    }

    @Override
    public void setLb(double lb) {
        super.setLb(Math.round(lb * precision) / precision);
    }

    @Override
    public void setUb(double ub) {
        super.setUb(Math.round(ub * precision) / precision);
    }

    @Override
    public void setBounds(double lb, double ub) {
        super.setBounds(Math.round(lb * precision) / precision, Math.round(ub * precision) / precision);
    }
}
