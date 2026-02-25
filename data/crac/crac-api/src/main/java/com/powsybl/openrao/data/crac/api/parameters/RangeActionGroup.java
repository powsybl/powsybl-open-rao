/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration object that defines a group of range actions which should be used
 * "in parallel" (i.e. with aligned setpoints)
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionGroup {
    private static final String SEPARATOR = " + ";

    private final List<String> rangeActionsIds;

    public RangeActionGroup(List<String> rangeActionsIds) {
        this.rangeActionsIds = rangeActionsIds;
    }

    public List<String> getRangeActionsIds() {
        return rangeActionsIds;
    }

    public static List<String> parse(String concatenatedIds) {
        String[] seperatedIds = concatenatedIds.split(Pattern.quote(SEPARATOR));
        if (seperatedIds.length < 2) {
            throw new OpenRaoException(
                String.format("ParallelRangeActions configuration %s cannot be interpreted, it should contains at least two ids seperated with '%s'", concatenatedIds, SEPARATOR)
            );
        } else {
            return Arrays.asList(seperatedIds);
        }
    }

    @Override
    public String toString() {
        return String.join(SEPARATOR, rangeActionsIds);
    }
}
