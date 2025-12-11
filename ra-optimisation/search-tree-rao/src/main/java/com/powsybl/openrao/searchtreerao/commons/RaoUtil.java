/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getLoadFlowProvider;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.addNetworkActionAssociatedWithHvdcRangeAction;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.updateHvdcRangeActionInitialSetpoint;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {
    private RaoUtil() {
    }

    public static void initData(final RaoInput raoInput, final RaoParameters raoParameters, final ReportNode reportNode) {
        checkParameters(raoParameters, raoInput, reportNode);
        checkCnecsThresholdsUnit(raoParameters, raoInput, reportNode);
        initNetwork(raoInput.getNetwork(), raoInput.getNetworkVariantId());
        updateHvdcRangeActionInitialSetpoint(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, reportNode);
        addNetworkActionAssociatedWithHvdcRangeAction(raoInput.getCrac(), raoInput.getNetwork());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
    }

    public static void checkParameters(final RaoParameters raoParameters,
                                       final RaoInput raoInput,
                                       final ReportNode reportNode) {
        checkObjectiveFunctionParameters(raoParameters, raoInput);
        checkLoopFlowParameters(raoParameters, raoInput, reportNode);

        if (!PstModel.APPROXIMATED_INTEGERS.equals(getPstModel(raoParameters))
            && raoInput.getCrac().getRaUsageLimitsPerInstant().values().stream().anyMatch(raUsageLimits -> !raUsageLimits.getMaxElementaryActionsPerTso().isEmpty())) {
            CommonReports.reportPstsMustBeApproximatedAsIntegers(reportNode);
            throw new OpenRaoException("The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.");
        }
    }

    private static void checkLoopFlowParameters(final RaoParameters raoParameters,
                                                final RaoInput raoInput,
                                                final ReportNode reportNode) {
        if ((raoParameters.getLoopFlowParameters().isPresent()
            || raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins())
            && (Objects.isNull(raoInput.getReferenceProgram()))) {
            CommonReports.reportNoReferenceProgramProvided(reportNode);
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), getLoadFlowProvider(raoParameters), getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters()));
        }

        if (raoParameters.getLoopFlowParameters().isPresent() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            CommonReports.reportLoopflowComputationLacksReferenceProgramOrGlskProvider(reportNode, raoInput.getCrac().getId());
            throw new OpenRaoException(format(
                "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoInput.getCrac().getId()));
        }
    }

    private static void checkObjectiveFunctionParameters(RaoParameters raoParameters, RaoInput raoInput) {
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

        if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization() &&
            (!raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) ||
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) && raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().isEmpty())) {
            throw new OpenRaoException(format("Objective function type %s requires a config with costly min margin parameters", raoParameters.getObjectiveFunctionParameters().getType()));
        }
    }

    public static void checkCnecsThresholdsUnit(final RaoParameters raoParameters,
                                                final RaoInput raoInput,
                                                final ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        if (!getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc()) {
            crac.getFlowCnecs().forEach(flowCnec -> {
                if (flowCnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getUnit().equals(Unit.MEGAWATT))) {
                    CommonReports.reportThresholdForFlowCnecDefinedInMwButLoadflowComputationIsInAc(reportNode, flowCnec.getId());
                }
            });
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
     * 1) The remedial action has no usage rule: it will not be available.
     * 2) It gathers all the remedial action usage rules and filters out the OnFlowConstraint(InCountry) with no negative margins on their associated cnecs.
     * If there are remaining usage rules, the remedial action is available.
     */
    public static boolean canRemedialActionBeUsed(RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, RaoParameters raoParameters) {
        return remedialAction.getUsageRules().stream().anyMatch(ur -> isUsageRuleActivated(ur, remedialAction, state, flowResult, flowCnecs, network, raoParameters.getObjectiveFunctionParameters().getUnit()));
    }

    private static boolean isUsageRuleActivated(UsageRule usageRule, RemedialAction<?> remedialAction, State state, FlowResult flowResult, Set<FlowCnec> flowCnecs, Network network, Unit unit) {
        if (usageRule instanceof OnInstant onInstant) {
            return onInstant.getInstant().equals(state.getInstant());
        } else if (usageRule instanceof OnContingencyState onContingencyState) {
            return onContingencyState.getState().equals(state);
        } else if (usageRule instanceof OnFlowConstraintInCountry onFlowConstraintInCountry) {
            if (onFlowConstraintInCountry.getContingency().isPresent() && !onFlowConstraintInCountry.getContingency().equals(state.getContingency())) {
                return false;
            }
            return isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(onFlowConstraintInCountry, flowCnecs, network), unit) && onFlowConstraintInCountry.getInstant().equals(state.getInstant());
        } else if (usageRule instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec flowCnec) {
            if (!onConstraint.getInstant().isPreventive() && !flowCnec.getState().getContingency().equals(state.getContingency())) {
                return false;
            }
            return isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(onConstraint, flowCnecs, network), unit) && onConstraint.getInstant().equals(state.getInstant());
        } else {
            return false;
        }
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
            State previousUsageState = optimizationContext.getMainOptimizationState();

            if (previousUsageState.getInstant().comesBefore(state.getInstant())) {
                Optional<RangeAction<?>> correspondingRa = optimizationContext.getRangeActionsPerState().get(previousUsageState).stream()
                    .filter(ra -> ra.getId().equals(rangeAction.getId()) || ra.getNetworkElements().equals(rangeAction.getNetworkElements()))
                    .findAny();

                if (correspondingRa.isPresent()) {
                    return Pair.of(correspondingRa.get(), previousUsageState);
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

    public static Set<String> getDuplicateCnecs(Set<FlowCnec> flowcnecs) {
        return flowcnecs.stream()
            .map(FlowCnec::getId)
            .filter(id -> id.contains("OUTAGE DUPLICATE"))
            .collect(Collectors.toSet());
    }

}
