/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoParameters;

import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoModeller {
    private LinearRaoData linearRaoData;
    private List<AbstractProblemFiller> fillerList;
    private List<AbstractPostProcessor> postProcessorList;
    private RaoParameters raoParameters;

    public LinearRaoModeller(LinearRaoData linearRaoData, List<AbstractProblemFiller> fillerList, List<AbstractPostProcessor> postProcessorList, RaoParameters raoParameters) {
        this.linearRaoData = linearRaoData;
        this.fillerList = fillerList;
        this.postProcessorList = postProcessorList;
        this.raoParameters = raoParameters;
    }

    public void buildProblem() {
        //todo
    }

    public void updateProblem(LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
    }

    public void solve() {
        //todo
    }
}
