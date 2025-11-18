/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.action.HvdcAction;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.impl.*;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper.computeActivePowerSetpointOnHvdcLine;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getLoadFlowProvider;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {
    private RaoUtil() {
    }

    public static void initData(RaoInput raoInput, RaoParameters raoParameters) {
        checkParameters(raoParameters, raoInput);
        checkCnecsThresholdsUnit(raoParameters, raoInput);
        initNetwork(raoInput.getNetwork(), raoInput.getNetworkVariantId());
        updateHvdcRangeActionInitialSetpoint(raoInput.getCrac(), raoInput.getNetwork(), raoParameters);
        addNetworkActionAssociatedWithHvdcRangeAction(raoInput.getCrac(), raoInput.getNetwork());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
    }

    public static void checkParameters(RaoParameters raoParameters, RaoInput raoInput) {
        checkObjectiveFunctionParameters(raoParameters, raoInput);
        checkLoopFlowParameters(raoParameters, raoInput);

        if (!PstModel.APPROXIMATED_INTEGERS.equals(getPstModel(raoParameters))
            && raoInput.getCrac().getRaUsageLimitsPerInstant().values().stream().anyMatch(raUsageLimits -> !raUsageLimits.getMaxElementaryActionsPerTso().isEmpty())) {
            String msg = "The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.";
            OpenRaoLoggerProvider.BUSINESS_LOGS.error(msg);
            throw new OpenRaoException(msg);
        }
    }

    private static void checkLoopFlowParameters(RaoParameters raoParameters, RaoInput raoInput) {
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

    public static void checkCnecsThresholdsUnit(RaoParameters raoParameters, RaoInput raoInput) {
        Crac crac = raoInput.getCrac();
        if (!getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc()) {
            crac.getFlowCnecs().forEach(flowCnec -> {
                if (flowCnec.getThresholds().stream().anyMatch(branchThreshold -> branchThreshold.getUnit().equals(Unit.MEGAWATT))) {
                    String msg = format("A threshold for the flowCnec %s is defined in MW but the loadflow computation is in AC. It will be imprecisely converted by the RAO which could create uncoherent results due to side effects", flowCnec.getId());
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn(msg);
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
            .filter(flowCnec -> flowCnec.getId().contains("OUTAGE DUPLICATE"))
            .map(FlowCnec::getId)
            .collect(Collectors.toSet());
    }

    /**
     * Add to the crac a network action that deactivate AC emulation for each HVDC line in AC emulation mode
     * that has at least one hvdc range action associated
     */
    public static void addNetworkActionAssociatedWithHvdcRangeAction(Crac crac, Network network) {
        // for each hvdc range action
        crac.getHvdcRangeActions().forEach(hvdcRangeAction -> {
            // get associated hvdc line id
            String hvdcLineId = hvdcRangeAction.getNetworkElement().getId();

            // Check if an AC emulation deactivation network action has already been created
            Set<NetworkAction> acEmulationDeactivationActionOnHvdcLine = crac.getNetworkActions().stream()
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action -> action instanceof HvdcAction))
                .filter(ra -> ra.getElementaryActions().stream().allMatch(action ->
                    ((HvdcAction) action).getHvdcId().equals(hvdcLineId)
                )).collect(Collectors.toSet());

            // get HVDC line's HvdcAngleDroopActivePowerControl extension
            HvdcAngleDroopActivePowerControl hvdcAngleDoopActivePowerControl = IidmHvdcHelper.getHvdcLine(network, hvdcLineId).getExtension(HvdcAngleDroopActivePowerControl.class);

            // if AC emulation is activated on HVDC line
            if (hvdcAngleDoopActivePowerControl != null && hvdcAngleDoopActivePowerControl.isEnabled()) {

                String networkActionId = String.format("%s_%s", "acEmulationDeactivation", hvdcLineId);
                if (acEmulationDeactivationActionOnHvdcLine.isEmpty()) {
                    // If the network action doesn't exist yet
                    // create a network action hvdcLineId_acEmulationDeactivation
                    // with one elementary action (an acEmulationDeactivationAction that uses under the hood powsybl's hvdcAction)
                    // and the same usage rule as the HVDC range action
                    NetworkActionAdder acEmulationDeactivationActionAdder = crac.newNetworkAction()
                        .withId(networkActionId)
                        .withOperator(hvdcRangeAction.getOperator())
                        .newAcEmulationDeactivationAction()
                        .withNetworkElement(hvdcLineId)
                        .add();
                    addAllUsageRules(hvdcRangeAction, acEmulationDeactivationActionAdder);
                    acEmulationDeactivationActionAdder.add();
                } else {
                    // If the network action already exists, just update the usage rules by adding hvdcRangeAction's ones.
                    NetworkAction acEmulationDeactivationAction = acEmulationDeactivationActionOnHvdcLine.iterator().next();
                    hvdcRangeAction.getUsageRules().stream().forEach(
                        usageRule -> acEmulationDeactivationAction.addUsageRule(usageRule)
                    );
                }
            }
        });
    }

    // Add all the usage rules of the range action to the network action
    static void addAllUsageRules(HvdcRangeAction hvdcRangeAction, NetworkActionAdder acEmulationDeactivationActionAdder) {
        hvdcRangeAction.getUsageRules().forEach(
            usageRule -> {
                if (usageRule.getClass().equals(OnInstantImpl.class)) {
                    acEmulationDeactivationActionAdder
                        .newOnInstantUsageRule()
                        .withInstant(usageRule.getInstant().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnConstraintImpl.class)) {
                    OnConstraint<?> onConstraint = (OnConstraint<?>) usageRule;
                    acEmulationDeactivationActionAdder.newOnConstraintUsageRule()
                        .withInstant(onConstraint.getInstant().getId())
                        .withCnec(onConstraint.getCnec().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnContingencyStateImpl.class)) {
                    OnContingencyState onContingencyState = (OnContingencyState) usageRule;
                    acEmulationDeactivationActionAdder.newOnContingencyStateUsageRule()
                        .withContingency(onContingencyState.getContingency().getId())
                        .withInstant(onContingencyState.getInstant().getId())
                        .add();
                } else if (usageRule.getClass().equals(OnFlowConstraintInCountryImpl.class)) {
                    OnFlowConstraintInCountry onFlowConstraintInCountry = (OnFlowConstraintInCountry) usageRule;
                    acEmulationDeactivationActionAdder.newOnFlowConstraintInCountryUsageRule()
                        .withCountry(onFlowConstraintInCountry.getCountry())
                        .withInstant(onFlowConstraintInCountry.getInstant().getId())
                        .withContingency(onFlowConstraintInCountry.getContingency().get().getId())
                        .add();
                }
            }
        );
    }

    static void updateHvdcRangeActionInitialSetpoint(Crac crac, Network network, RaoParameters raoParameters) {
        // get all the hvdc range action that uses hvdc line in ac emulation mode
        // TODO: put in common
        Set<HvdcRangeActionImpl> hvdcRangeActionOnAcEmulationHvdcLinecrac = crac.getHvdcRangeActions().stream()
            .map(HvdcRangeActionImpl.class::cast)
            .filter(hvdcRangeAction -> hvdcRangeAction.isAngleDroopActivePowerControlEnabled(network))
            .collect(Collectors.toSet());

        if (!hvdcRangeActionOnAcEmulationHvdcLinecrac.isEmpty()) {
            // Run load flow to update flow on all the lines of the network
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)){
                loadFlowAndSensitivityParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters();
            }
            LoadFlow.find(loadFlowAndSensitivityParameters.getLoadFlowProvider()).run(network, loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
            hvdcRangeActionOnAcEmulationHvdcLinecrac.stream().forEach(hvdcRangeAction -> {
                String hvdcLineId = hvdcRangeAction.getNetworkElement().getId();
                HvdcLine hvdcLine = IidmHvdcHelper.getHvdcLine(network, hvdcLineId);
                double activePowerSetpoint = computeActivePowerSetpointOnHvdcLine(hvdcLine);
                // is NaN if the line is disconnected
                if (activePowerSetpoint != Double.NaN) {
                    hvdcLine.setConvertersMode(activePowerSetpoint > 0 ? HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER : HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
                    hvdcLine.setActivePowerSetpoint(Math.abs(activePowerSetpoint));
                    hvdcRangeAction.setInitialSetpoint(activePowerSetpoint);
                }
            });

        }
    }
}
