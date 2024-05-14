/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
            throw new OpenRaoException(format("Objective function %s cannot be calculated with a DC default sensitivity engine", raoParameters.getObjectiveFunctionParameters().getType().toString()));
        }

        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoInput.getGlskProvider() == null) {
                throw new OpenRaoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunctionParameters().getType()));
            }
            if (!raoParameters.hasExtension(RelativeMarginsParametersExtension.class) || raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().isEmpty()) {
                throw new OpenRaoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunctionParameters().getType()));
            }
        }

        if ((raoParameters.hasExtension(LoopFlowParametersExtension.class)
                || raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins())
                && (Objects.isNull(raoInput.getReferenceProgram()))) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider(), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters(), ReportNode.NO_OP));
        }

        if (raoParameters.hasExtension(LoopFlowParametersExtension.class) && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                    "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                    raoInput.getCrac().getId());
            OpenRaoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new OpenRaoException(msg);
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
            throw new OpenRaoException("Only conversions between MW and A are supported.");
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
     * Evaluates if a remedial action is available.
     * 1) The remedial action has no usage rule:
     * It will not be available.
     * 2) It gathers all the remedial action usageMethods and filters out the OnFlowConstraint(InCountry) with no negative margins on their associated cnecs.
     * 3) It computes the "strongest" usage method.
     * The remedial action is available if and only if the usage method is "AVAILABLE".
     */
    public static boolean isRemedialActionAvailable(RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, RaoParameters raoParameters) {
        UsageMethod finalUsageMethod = getFinalUsageMethod(remedialAction, state, flowResult, flowCnecs, network, raoParameters);
        return finalUsageMethod != null && finalUsageMethod.equals(UsageMethod.AVAILABLE);
    }

    /**
     * Evaluates if a remedial action is forced.
     * 1) The remedial action has no usage rule:
     * It will not be forced.
     * 2) It gathers all the remedial action usageMethods and filters out the OnFlowConstraint(InCountry) with no negative margins on their associated cnecs.
     * 3) It computes the "strongest" usage method.
     * For automatonState, the remedial action is forced if and only if the usage method is "FORCED".
     * For non-automaton states, a forced remedial action is not supported and the remedial action is ignored.
     */
    public static boolean isRemedialActionForced(RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, RaoParameters raoParameters) {
        UsageMethod finalUsageMethod = getFinalUsageMethod(remedialAction, state, flowResult, flowCnecs, network, raoParameters);
        if (finalUsageMethod == null) {
            return false;
        }
        if (!state.getInstant().isAuto() && finalUsageMethod.equals(UsageMethod.FORCED)) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn(format("The 'forced' usage method is for automatons only. Therefore, %s will be ignored for this state: %s", remedialAction.getName(), state.getId()));
            return false;
        }
        return finalUsageMethod.equals(UsageMethod.FORCED);
    }

    private static UsageMethod getFinalUsageMethod(RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, RaoParameters raoParameters) {
        Set<UsageRule> usageRules = remedialAction.getUsageRules();
        if (usageRules.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn(format("The remedial action %s has no usage rule and therefore will not be available.", remedialAction.getName()));
            return null;
        }

        Set<UsageMethod> usageMethods = getAllUsageMethods(usageRules, remedialAction, state, flowResult, flowCnecs, network, raoParameters);
        return UsageMethod.getStrongestUsageMethod(usageMethods);
    }

    /**
     * Returns a set of usageMethods corresponding to a remedialAction.
     * It filters out every OnFlowConstraint(InCountry) that is not applicable due to positive margins.
     */
    private static Set<UsageMethod> getAllUsageMethods(Set<UsageRule> usageRules, RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, RaoParameters raoParameters) {
        return usageRules.stream()
            .filter(ur -> ur instanceof OnContingencyState || ur instanceof OnInstant
                || (ur instanceof OnFlowConstraint || ur instanceof OnFlowConstraintInCountry)
                && isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(ur, flowCnecs, network), raoParameters.getObjectiveFunctionParameters().getType().getUnit()))
            .map(ur -> ur.getUsageMethod(state))
            .collect(Collectors.toSet());
    }

    /**
     * Returns the range action from optimizationContext that is available on the latest state
     * strictly before the given state, and that acts on the same network element as rangeAction.
     */
    public static Pair<RangeAction<?>, State> getLastAvailableRangeActionOnSameNetworkElement(OptimizationPerimeter optimizationContext, RangeAction<?> rangeAction, State state) {

        if (state.isPreventive() || state.equals(optimizationContext.getMainOptimizationState())) {
            // no previous instant
            return null;
        } else if (state.getInstant().isCurative()) {

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
            throw new OpenRaoException("Linear optimization does not handle range actions which are neither PREVENTIVE nor CURATIVE.");
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

    public static void applyRemedialActions(Network network, OptimizationResult optResult, State state) {
        optResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        optResult.getActivatedRangeActions(state).forEach(rangeAction -> rangeAction.apply(network, optResult.getOptimizedSetpoint(rangeAction, state)));
    }
}
