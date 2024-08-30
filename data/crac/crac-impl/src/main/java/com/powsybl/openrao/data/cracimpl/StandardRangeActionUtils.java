/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.range.StandardRange;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * Common code for StandardRangeAction implementations (adding another abstract class for standard range actions would be too much class depth)
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com}
 */
public final class StandardRangeActionUtils {

    private StandardRangeActionUtils() {
    }

    static double getMinAdmissibleSetpoint(double previousInstantSetPoint, List<StandardRange> ranges, double initialSetpoint) {
        double minAdmissibleSetpoint = Double.NEGATIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, range.getMin());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, initialSetpoint + range.getMin());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, previousInstantSetPoint + range.getMin());
                    break;
                // to avoid throwing exception
                case RELATIVE_TO_PREVIOUS_TIME_STEP:
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return minAdmissibleSetpoint;
    }

    static double getMaxAdmissibleSetpoint(double previousInstantSetPoint, List<StandardRange> ranges, double initialSetpoint) {
        double maxAdmissibleSetpoint = Double.POSITIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, range.getMax());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, initialSetpoint + range.getMax());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, previousInstantSetPoint + range.getMax());
                    break;
                // to avoid throwing exception
                case RELATIVE_TO_PREVIOUS_TIME_STEP:
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return maxAdmissibleSetpoint;
    }
}
