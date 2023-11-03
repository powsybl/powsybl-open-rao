/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        if (raoParameters.getObjectiveFunctionParameters().getType().getUnit().equals(Unit.AMPERE)
                && raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().isDc()) {
            throw new FaraoException(format("Objective function %s cannot be calculated with a DC default sensitivity engine", raoParameters.getObjectiveFunctionParameters().getType().toString()));
        }

        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoInput.getGlskProvider() == null) {
                throw new FaraoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunctionParameters().getType()));
            }
            if (!raoParameters.hasExtension(RelativeMarginsParametersExtension.class) || raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().isEmpty()) {
                throw new FaraoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunctionParameters().getType()));
            }
        }

        if ((raoParameters.hasExtension(LoopFlowParametersExtension.class)
                || raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins())
                && (Objects.isNull(raoInput.getReferenceProgram()))) {
            FaraoLoggerProvider.BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider(), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters()));
        }

        if (raoParameters.hasExtension(LoopFlowParametersExtension.class) && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
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

    /**
     * Returns true if any flowCnec has a negative margin.
     * We need to know the unit of the objective function, because a negative margin in A can be positive in MW
     * given different approximations, and vice versa
     */
    public static boolean isAnyMarginNegative(FlowResult flowResult, Set<FlowCnec> flowCnecs, Unit marginUnit) {
        return flowCnecs.stream().anyMatch(flowCnec -> flowResult.getMargin(flowCnec, marginUnit) <= 0);
    }

    /**
     * Returns the range action from optimizationContext that is available on the latest state
     * strictly before the given state, and that acts on the same network element as rangeAction.
     */
    public static Pair<RangeAction<?>, State> getLastAvailableRangeActionOnSameNetworkElement(OptimizationPerimeter optimizationContext, RangeAction<?> rangeAction, State state) {

        if (state.isPreventive() || state.equals(optimizationContext.getMainOptimizationState())) {
            // no previous instant
            return null;
        } else if (state.getInstant().getInstantKind().equals(InstantKind.CURATIVE)) {

            // look if a preventive range action acts on the same network elements
            State preventiveState = optimizationContext.getMainOptimizationState();

            if (preventiveState.isPreventive()) {
                Optional<RangeAction<?>> correspondingRa = optimizationContext.getRangeActionsPerState().get(preventiveState).stream()
                    .filter(ra -> ra.getId().equals(rangeAction.getId()) || ra.getNetworkElements().equals(rangeAction.getNetworkElements()))
                    .findAny();

                if (correspondingRa.isPresent()) {
                    return Pair.of(correspondingRa.get(), preventiveState);
                }
            }
            return null;
        } else {
            throw new FaraoException("Linear optimization does not handle range actions which are neither PREVENTIVE nor CURATIVE.");
        }
    }

    public static double getLargestCnecThreshold(Set<FlowCnec> flowCnecs, Unit unit) {
        return flowCnecs.stream().filter(Cnec::isOptimized)
            .map(flowCnec ->
                flowCnec.getMonitoredSides().stream().map(side ->
                    Math.max(Math.abs(flowCnec.getUpperBound(side, unit).orElse(0.)), Math.abs(flowCnec.getLowerBound(side, unit).orElse(0.)))).max(Double::compare).orElse(0.))
            .max(Double::compare)
            .orElse(0.);
    }

    public static boolean cnecShouldBeOptimized(Map<FlowCnec, RangeAction<?>> flowCnecPstRangeActionMap,
                                                FlowResult flowResult,
                                                FlowCnec flowCnec,
                                                Side side,
                                                RangeActionActivationResult rangeActionActivationResult,
                                                RangeActionSetpointResult prePerimeterRangeActionSetpointResult,
                                                SensitivityResult sensitivityResult,
                                                Unit unit) {
        return cnecShouldBeOptimized(flowCnecPstRangeActionMap, flowResult, flowCnec, side, rangeActionActivationResult.getOptimizedSetpointsOnState(flowCnec.getState()), prePerimeterRangeActionSetpointResult, sensitivityResult, unit);
    }

    public static boolean cnecShouldBeOptimized(Map<FlowCnec, RangeAction<?>> flowCnecPstRangeActionMap,
                                                FlowResult flowResult,
                                                FlowCnec flowCnec,
                                                Side side,
                                                Map<RangeAction<?>, Double> activatedRangeActionsWithSetpoint,
                                                RangeActionSetpointResult prePerimeterRangeActionSetpointResult,
                                                SensitivityResult sensitivityResult,
                                                Unit unit) {
        if (!flowCnecPstRangeActionMap.containsKey(flowCnec)) {
            return true;
        }

        RangeAction<?> ra = flowCnecPstRangeActionMap.get(flowCnec);
        double cnecMarginToUpperBound = flowCnec.getUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - flowResult.getFlow(flowCnec, side, unit);
        double cnecMarginToLowerBound = flowResult.getFlow(flowCnec, side, unit) - flowCnec.getLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY);
        if (cnecMarginToUpperBound >= 0 && cnecMarginToLowerBound >= 0) {
            return false;
        }

        double sensitivity = sensitivityResult.getSensitivityValue(flowCnec, side, ra, Unit.MEGAWATT) * getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, unit);
        double raCurrentSetpoint = activatedRangeActionsWithSetpoint.getOrDefault(ra, prePerimeterRangeActionSetpointResult.getSetpoint(ra));
        double raMaxDecrease = raCurrentSetpoint - ra.getMinAdmissibleSetpoint(prePerimeterRangeActionSetpointResult.getSetpoint(ra));
        double raMaxIncrease = ra.getMaxAdmissibleSetpoint(prePerimeterRangeActionSetpointResult.getSetpoint(ra)) - raCurrentSetpoint;
        double maxFlowDecrease = sensitivity >= 0 ? sensitivity * raMaxDecrease : -sensitivity * raMaxIncrease;
        double maxFlowIncrease = sensitivity >= 0 ? sensitivity * raMaxIncrease : -sensitivity * raMaxDecrease;

        return cnecMarginToUpperBound + maxFlowDecrease < 0 || cnecMarginToLowerBound + maxFlowIncrease < 0;
    }
}
