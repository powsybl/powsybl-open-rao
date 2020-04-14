/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.optimisation;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public abstract class AbstractProblemFiller {
    protected LinearRaoProblem linearRaoProblem;
    protected LinearRaoData linearRaoData;

    public AbstractProblemFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        this.linearRaoProblem = linearRaoProblem;
        this.linearRaoData = linearRaoData;
    }

    public void setLinearRaoProblem(LinearRaoProblem linearRaoProblem) {
        this.linearRaoProblem = linearRaoProblem;
    }

    public void setLinearRaoData(LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
    }

    public abstract void fill();

    public abstract void update();
}
