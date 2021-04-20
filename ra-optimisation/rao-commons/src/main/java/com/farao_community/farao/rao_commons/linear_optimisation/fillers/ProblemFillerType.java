/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum ProblemFillerType {
    CORE,
    MAX_MIN_MARGIN,
    MAX_MIN_RELATIVE_MARGIN,
    MAX_LOOP_FLOW,
    MNEC,
    UNOPTIMIZED_CNEC
}
