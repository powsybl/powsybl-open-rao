package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.range.StandardRange;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com}
 * Common code for StandradRangeAction implementations (adding another abstract class for standard range actions would be too much class depth)
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
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return maxAdmissibleSetpoint;
    }
}
