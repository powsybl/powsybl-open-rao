/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.optimisation;

import com.farao_community.farao.linear_rao.Situation;
import com.farao_community.farao.rao_api.RaoResult;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public abstract class AbstractPostProcessor {

    public abstract void process(LinearRaoProblem linearRaoProblem, Situation situation, RaoResult raoResult);
}
