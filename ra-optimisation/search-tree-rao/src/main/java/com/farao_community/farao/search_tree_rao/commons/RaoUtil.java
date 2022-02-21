/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {
    private RaoUtil() {
    }

    public static void initData(RaoInput raoInput, RaoParameters raoParameters) {
        checkParameters(raoParameters, raoInput);
        initNetwork(raoInput.getNetwork(), raoInput.getNetworkVariantId());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
    }

    public static void checkParameters(RaoParameters raoParameters, RaoInput raoInput) {

        if (raoParameters.getObjectiveFunction().getUnit().equals(Unit.AMPERE)
                && raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            throw new FaraoException(format("Objective function %s cannot be calculated with a DC default sensitivity engine", raoParameters.getObjectiveFunction().toString()));
        }

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            if (raoInput.getGlskProvider() == null) {
                throw new FaraoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunction()));
            }
            if (raoParameters.getRelativeMarginPtdfBoundaries().isEmpty()) {
                throw new FaraoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunction()));
            }
        }

        if ((raoParameters.isRaoWithLoopFlowLimitation()
                || raoParameters.getObjectiveFunction().doesRequirePtdf())
                && (raoInput.getReferenceProgram() == null)) {
            FaraoLoggerProvider.BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getLoadFlowProvider(), raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters()));
        }

        if (raoParameters.isRaoWithLoopFlowLimitation() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                    "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                    raoInput.getCrac().getId());
            FaraoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new FaraoException(msg);
        }
    }

    public static double getFlowUnitMultiplier(FlowCnec cnec, Side voltageSide, Unit unitFrom, Unit unitTo) {
        if (unitFrom == unitTo) {
            return 1;
        }
        double nominalVoltage = cnec.getNominalVoltage(voltageSide);
        if (unitFrom == Unit.MEGAWATT && unitTo == Unit.AMPERE) {
            return 1000 / (nominalVoltage * Math.sqrt(3));
        } else if (unitFrom == Unit.AMPERE && unitTo == Unit.MEGAWATT) {
            return nominalVoltage * Math.sqrt(3) / 1000;
        } else {
            throw new FaraoException("Only conversions between MW and A are supported.");
        }
    }

    /* Method used to make sure the LP is reproducible. This basically rounds the least significant bits of a double.
     Let's say a double has 10 precision bits (in reality, 52)
     We take an initial double:
       .............//////////.....
     To which we add a "bigger" double :
       .........\\\\\\\\\\..........
      =>
       .........\\\\||||||..........
       (we "lose" the least significant bits of the first double because the sum double doesn't have enough precision to show them)
     Then we substract the same "bigger" double:
       .............//////..........
       We get back our original bits for the most significant part, but the least significant bits are still gone.
     */

    public static double roundDouble(double value, int numberOfBitsToRoundOff) {
        double t = value * (1L << numberOfBitsToRoundOff);
        if (t != Double.POSITIVE_INFINITY && value != Double.NEGATIVE_INFINITY && !Double.isNaN(t)) {
            return value - t + t;
        }
        return value;
    }
}
