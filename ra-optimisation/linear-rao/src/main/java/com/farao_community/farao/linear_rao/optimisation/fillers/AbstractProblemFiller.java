/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.optimisation.fillers;

import com.farao_community.farao.linear_rao.Situation;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public abstract class AbstractProblemFiller {

    public abstract void fill(Situation situation, LinearRaoProblem linearRaoProblem);

    public abstract void update(Situation situation, LinearRaoProblem linearRaoProblem);
}
