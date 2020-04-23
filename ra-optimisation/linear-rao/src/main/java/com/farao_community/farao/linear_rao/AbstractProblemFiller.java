/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.linear_rao.config.LinearRaoParameters;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public abstract class AbstractProblemFiller {
    protected LinearRaoProblem linearRaoProblem;
    protected LinearRaoData linearRaoData;
    protected LinearRaoParameters linearRaoParameters;

    public AbstractProblemFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, LinearRaoParameters linearRaoParameters) {
        this.linearRaoProblem = linearRaoProblem;
        this.linearRaoData = linearRaoData;
        this.linearRaoParameters = linearRaoParameters;
    }

    public abstract void fill();

    public abstract void update();
}
