/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        checkCurativeRaUsageLimit(raoInput.getCrac());
    }

    /**
     * Generates a mapping of TSOs to their respective limit values for a given limit type, with values organized chronologically by instants.
     * ex. {"BE": [1, 2, 3], "FR": [2, null, 3]}
     */
    public static Map<String, List<Integer>> dataPerTsoForLimit(Crac crac, String limitType, List<Instant> sortedCurativeInstants) {

        // Collect for each TSO a list of limit values sorted by instant
        Map<String, List<Integer>> dataPerTso = new HashMap<>();
        for (Instant instant : sortedCurativeInstants) {
            RaUsageLimits raUsageLimits = crac.getRaUsageLimits(instant);
            if (raUsageLimits != null) {
                getRaUsageLimitByLimitName(limitType, raUsageLimits).forEach((tso, limit) ->
                    dataPerTso.computeIfAbsent(tso, k -> new ArrayList<>()).add(limit == null ? null : limit)
                );
            }

            for (Map.Entry<String, List<Integer>> entry : dataPerTso.entrySet()) {
                if (raUsageLimits == null || !getRaUsageLimitByLimitName(limitType, raUsageLimits).containsKey(entry.getKey())) {
                    entry.getValue().add(null);
                }
            }
        }

        return dataPerTso;
    }

    private static Map<String, Integer> getRaUsageLimitByLimitName(String limitType, RaUsageLimits raUsageLimits) {
        switch (limitType) {
            case "maxRaPerTso":
                return raUsageLimits.getMaxRaPerTso();
            case "maxPstPerTso":
                return raUsageLimits.getMaxPstPerTso();
            case "maxTopoPerTso":
                return raUsageLimits.getMaxTopoPerTso();
            case "maxElementaryActionsPerTso":
                return raUsageLimits.getMaxElementaryActionsPerTso();
            case "maxRa":
                return Collections.singletonMap(null, raUsageLimits.getMaxRa());
            default:
                return new HashMap<>();
        }
    }

    /**
     * Check that remedial action usage limits are coherent.
     * We do not allow null values for an instant if a limit was defined for a previous and following instant
     * ex. if curative2 has no max-ra limit for TSO "FR" but curative1 and curative3 have a limit -> throw an error
     */
    public static void checkCurativeRaUsageLimit(Crac crac) {
        List<String> raUsageLimitToCheck = List.of("maxRaPerTso", "maxTopoPerTso", "maxPstPerTso", "maxElementaryActionsPerTso", "maxRa");
        List<Instant> sortedCurativeInstants = crac.getSortedInstants().stream()
            .filter(Instant::isCurative)
            .collect(Collectors.toList());

        for (String limitType : raUsageLimitToCheck) {
            Map<String, List<Integer>> dataPerTso = dataPerTsoForLimit(crac, limitType, sortedCurativeInstants);

            for (String tso : dataPerTso.keySet()) {
                List<Integer> values = dataPerTso.get(tso);

                boolean foundNonNull = false; // Tracks if at least one non-null has been seen in the current sequence

                for (int i = 0; i < values.size(); i++) {
                    Integer value = values.get(i);

                    if (value == null) {
                        if (foundNonNull) {
                            // Check if the sequence has begun and a null violates the rule
                            for (int j = i + 1; j < values.size(); j++) {
                                if (values.get(j) != null) {
                                    if (tso == null) {
                                        throw new OpenRaoException(String.format(
                                            "Incoherence found for limit '%s' null value found between non-null values at instant %s.", limitType, sortedCurativeInstants.get(i).getId()
                                        ));
                                    }
                                    throw new OpenRaoException(String.format(
                                        "Incoherence found for limit '%s' null value found between non-null values for TSO '%s' at instant %s.", limitType, tso, sortedCurativeInstants.get(i).getId()
                                    ));
                                }
                            }
                        }
                        break;
                    } else {
                        foundNonNull = true;
                    }
                }
            }
        }

    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
    }

    public static void checkParameters(final RaoParameters raoParameters,
                                       final RaoInput raoInput,
                                       final ReportNode reportNode) {
        checkObjectiveFunctionParameters(raoParameters, raoInput);
        checkLoopFlowParameters(raoParameters, raoInput, reportNode);
        checkHvdcAcEmulationParameters(raoParameters, raoInput, reportNode);

        if (!PstModel.APPROXIMATED_INTEGERS.equals(getPstModel(raoParameters))
            && raoInput.getCrac().getRaUsageLimitsPerInstant().values().stream().anyMatch(raUsageLimits -> !raUsageLimits.getMaxElementaryActionsPerTso().isEmpty())) {
            CommonReports.reportPstsMustBeApproximatedAsIntegers(reportNode);
            throw new OpenRaoException("The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.");
        }
    }

    public static void checkHvdcAcEmulationParameters(final RaoParameters raoParameters,
                                                      final RaoInput raoInput,
                                                      final ReportNode reportNode) {
        boolean isAnyHvdcInAcEmulation = raoInput.getNetwork().getHvdcLineStream()
            .anyMatch(hvdcLine -> {
                HvdcAngleDroopActivePowerControl extension = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
                return extension != null && extension.isEnabled();
            });

        if (!getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isHvdcAcEmulation() && isAnyHvdcInAcEmulation) {
            CommonReports.reportHvdcAcEmulationDisabledButNetworkContainsAcHvdcLines(reportNode);
            throw new OpenRaoException("hvdcAcEmulation is not enabled but some HVDC lines are in AC emulation mode which will not be coherent.");
        }
    }

    private static void checkLoopFlowParameters(final RaoParameters raoParameters,
                                                final RaoInput raoInput,
                                                final ReportNode reportNode) {
        if ((raoParameters.getLoopFlowParameters().isPresent()
            || raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins())
            && (Objects.isNull(raoInput.getReferenceProgram()))) {
            CommonReports.reportNoReferenceProgramProvided(reportNode);
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(
                raoInput.getNetwork(),
                getLoadFlowProvider(raoParameters),
                getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters())
            );
        }

        if (raoParameters.getLoopFlowParameters().isPresent() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            CommonReports.reportLoopflowComputationLacksReferenceProgramOrGlskProvider(reportNode, raoInput.getCrac().getId());
            throw new OpenRaoException(format(
                "Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoInput.getCrac().getId()));
        }
    }

    private static void checkObjectiveFunctionParameters(RaoParameters raoParameters, RaoInput raoInput) {

        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoInput.getGlskProvider() == null) {
                throw new OpenRaoException(format("Objective function %s requires glsks", raoParameters.getObjectiveFunctionParameters().getType()));
            }
            if (raoParameters.getRelativeMarginsParameters().map(relativeMarginsParameters -> relativeMarginsParameters.getPtdfBoundaries().isEmpty()).orElse(true)) {
                throw new OpenRaoException(format("Objective function %s requires a config with a non empty boundary set", raoParameters.getObjectiveFunctionParameters().getType()));
            }
        }

        if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization() &&
            (!raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                || raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) && raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().isEmpty())) {
            throw new OpenRaoException(format("Objective function type %s requires a config with costly min margin parameters",
                                              raoParameters.getObjectiveFunctionParameters().getType()));
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
        return remedialAction.getUsageRules().stream().anyMatch(ur -> isUsageRuleActivated(ur, remedialAction, state, flowResult, flowCnecs, network, getFlowUnit(raoParameters)));
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
            return isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(onFlowConstraintInCountry, flowCnecs, network), unit)
                && onFlowConstraintInCountry.getInstant().equals(state.getInstant());
        } else if (usageRule instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec flowCnec) {
            if (!onConstraint.getInstant().isPreventive() && !flowCnec.getState().getContingency().equals(state.getContingency())) {
                return false;
            }
            return isAnyMarginNegative(flowResult, remedialAction.getFlowCnecsConstrainingForOneUsageRule(onConstraint, flowCnecs, network), unit)
                && onConstraint.getInstant().equals(state.getInstant());
        } else {
            return false;
        }
    }

    /**
     * Returns the range action from optimizationContext that is available on the latest state
     * strictly before the given state, and that acts on the same network element as rangeAction.
     * ex. in 2P multi-curative, if a PST is available in curative2 and curative3, for state-curative3, the function will return Pair.of(state-curative2, PST)
     */
    public static Pair<RangeAction<?>, State> getLastAvailableRangeActionOnSameNetworkElement(OptimizationPerimeter optimizationContext, RangeAction<?> rangeAction, State state) {

        if (state.isPreventive() || state.equals(optimizationContext.getMainOptimizationState())) {
            // no previous instant
            return null;
        } else if (state.getInstant().isCurative()) {

            // look if a previous instant (preventive or previous curative instant) range action acts on the same network elements
            Optional<State> previousUsageStateOptional = optimizationContext.getRangeActionsPerState()
                .keySet().stream()
                .filter(state1 -> state1.getInstant().comesBefore(state.getInstant()))
                .filter(state1 -> state1.getContingency().equals(state.getContingency()) || state1.getContingency().isEmpty())
                .sorted(
                    Comparator.comparing(
                        (State e) ->
                            e.getInstant().getOrder()
                    ).reversed()
                )
                .findFirst();

            if (previousUsageStateOptional.isPresent()) {
                Optional<RangeAction<?>> correspondingRa = optimizationContext.getRangeActionsPerState().get(previousUsageStateOptional.get()).stream()
                    .filter(ra -> ra.getId().equals(rangeAction.getId()) || ra.getNetworkElements().equals(rangeAction.getNetworkElements()))
                    .findAny();

                if (correspondingRa.isPresent()) {
                    return Pair.of(correspondingRa.get(), previousUsageStateOptional.get());
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

    public static void applyContingency(Network network, State state) {
        if (state.getContingency().isPresent()) {
            Contingency contingency = state.getContingency().orElseThrow();
            if (!contingency.isValid(network)) {
                throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
            }
            contingency.toModification().apply(network, (ComputationManager) null);
        }
    }

    public static Set<String> getDuplicateCnecs(Set<FlowCnec> flowcnecs) {
        return flowcnecs.stream()
            .map(FlowCnec::getId)
            .filter(id -> id.contains("OUTAGE DUPLICATE"))
            .collect(Collectors.toSet());
    }

    public static int getNumberOfConnectedComponent(Network network) {
        return Math.toIntExact(StreamSupport.stream(network.getBusBreakerView().getBuses().spliterator(), false)
            .map(Bus::getConnectedComponent)
            .distinct()
            .count());
    }

    // TODO: find a better place for this function
    public static Unit getFlowUnit(final RaoParameters raoParameters) {
        return getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc() ? Unit.MEGAWATT : Unit.AMPERE;
    }

}
