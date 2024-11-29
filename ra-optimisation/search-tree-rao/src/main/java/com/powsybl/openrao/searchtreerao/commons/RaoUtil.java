/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getLoadFlowProvider;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.getPstModel;
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
        if (raoParameters.getObjectiveFunctionParameters().getUnit().equals(Unit.AMPERE)
                && getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc()) {
            throw new OpenRaoException(format("Objective function unit %s cannot be calculated with a DC default sensitivity engine", raoParameters.getObjectiveFunctionParameters().getUnit().toString()));
        }

        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoInput.getGlskProvider() == null) {
                throw new OpenRaoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunctionParameters().getType()));
            }
            if (raoParameters.getRelativeMarginsParameters().map(relativeMarginsParameters -> relativeMarginsParameters.getPtdfBoundaries().isEmpty()).orElse(true)) {
                throw new OpenRaoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunctionParameters().getType()));
            }
        }

        if ((raoParameters.getLoopFlowParameters().isPresent()
                || raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins())
                && (Objects.isNull(raoInput.getReferenceProgram()))) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), getLoadFlowProvider(raoParameters), getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters()));
        }

        if (raoParameters.getLoopFlowParameters().isPresent() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                    "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                    raoInput.getCrac().getId());
            OpenRaoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new OpenRaoException(msg);
        }

        if (!PstModel.APPROXIMATED_INTEGERS.equals(getPstModel(raoParameters))
            && raoInput.getCrac().getRaUsageLimitsPerInstant().values().stream().anyMatch(raUsageLimits -> !raUsageLimits.getMaxElementaryActionsPerTso().isEmpty())) {
            String msg = "The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.";
            OpenRaoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new OpenRaoException(msg);
        }
    }

    public static double getFlowUnitMultiplier(FlowCnec cnec, TwoSides voltageSide, Unit unitFrom, Unit unitTo) {
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
                || (ur instanceof OnFlowConstraintInCountry || ur instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec)
                && isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(ur, flowCnecs, network), raoParameters.getObjectiveFunctionParameters().getUnit()))
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

    public static void applyRemedialActions(Network network, OptimizationResult optResult, State state) {
        optResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        optResult.getActivatedRangeActions(state).forEach(rangeAction -> rangeAction.apply(network, optResult.getOptimizedSetpoint(rangeAction, state)));
    }
}
