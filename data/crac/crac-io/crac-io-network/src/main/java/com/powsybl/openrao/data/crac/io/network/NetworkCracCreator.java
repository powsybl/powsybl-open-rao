/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.util.SwitchPredicates;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.PstHelper;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.crac.io.network.parameters.*;
import org.jgrapht.alg.util.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a CRAC from a network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreator {
    private NetworkCracCreationContext creationContext;
    private CracCreationParameters cracCreationParameters;
    private NetworkCracCreationParameters specificParameters;
    private Network network;
    private Crac crac;
    private final Map<Branch<?>, Contingency> contingencyPerBranch = new HashMap<>();

    NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        String cracId = "CRAC_FROM_NETWORK_" + network.getNameOrId();
        this.network = network;
        crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());
        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        this.cracCreationParameters = cracCreationParameters;
        specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);
        addInstants();
        addContingencies();
        addCnecsAndMnecs();
        addPstRangeActions();
        addRedispatchRangeActions();
        creationContext.setCreationSuccessful(true);
        return creationContext;
    }

    private void addCnecsAndMnecs() {
        Pair<Set<Branch<?>>, Set<Branch<?>>> criticalBranches = initCriticalBranches();
        addCnecs(criticalBranches.getFirst(), true);
        addCnecs(criticalBranches.getSecond(), false);
    }

    private Pair<Set<Branch<?>>, Set<Branch<?>>> initCriticalBranches() {
        CriticalElements params = specificParameters.getCriticalElements();
        Set<Branch<?>> optimizedBranches = new HashSet<>();
        Set<Branch<?>> monitoredBranches = new HashSet<>();
        network.getBranchStream()
            .filter(b ->
                Utils.branchIsInCountries(b, params.getCountries().orElse(null))
                    && ((cracCreationParameters.getDefaultMonitoredSides().contains(TwoSides.ONE) && b.getSelectedOperationalLimitsGroup1().isPresent()) || (cracCreationParameters.getDefaultMonitoredSides().contains(TwoSides.TWO) && b.getSelectedOperationalLimitsGroup2().isPresent()))
            ).forEach(branch -> {
                if (Utils.branchIsInVRange(branch, params.getOptimizedMinV(), params.getOptimizedMaxV())) {
                    optimizedBranches.add(branch);
                } else if (params.getMinAndMaxMonitoredV().isPresent() && Utils.branchIsInVRange(branch, params.getMinAndMaxMonitoredV().get().getMin(), params.getMinAndMaxMonitoredV().get().getMax())) {
                    monitoredBranches.add(branch);
                }
            });
        return Pair.of(optimizedBranches, monitoredBranches);
    }

    private void addInstants() {
        specificParameters.getInstants().forEach((instantKind, ids) ->
            ids.forEach(id -> crac.newInstant(id, instantKind)));
    }


    private void addContingencies() {
        Contingencies params = specificParameters.getContingencies();
        network.getBranchStream().filter(b ->
                Utils.branchIsInCountries(b, params.getCountries().orElse(null))
                    && Utils.branchIsInVRange(b, params.getMinV(), params.getMaxV()))
            .forEach(
                branch -> {
                    Contingency contingency = crac.newContingency()
                        .withId("CO_" + branch.getNameOrId())
                        .withContingencyElement(branch.getId(), ContingencyElementType.BRANCH)
                        .add();
                    contingencyPerBranch.put(branch, contingency);
                }
            );
    }

    private void addCnecs(Set<Branch<?>> branches, boolean optimized) {
        branches.forEach(branch -> {
            try {
                addPreventiveCnec(branch, optimized, !optimized);
                for (Contingency contingency : crac.getContingencies()) {
                    if (!specificParameters.getCriticalElements().shouldCreateCnec(branch, contingency)
                        || (contingencyPerBranch.containsKey(branch) && contingencyPerBranch.get(branch).equals(contingency))) {
                        continue;
                    }
                    addPostContingencyCnec(branch, contingency, optimized, !optimized, crac.getOutageInstant());
                    for (String curativeInstantId : specificParameters.getInstants().get(InstantKind.CURATIVE)) {
                        addPostContingencyCnec(branch, contingency, optimized, !optimized, crac.getInstant(curativeInstantId));
                    }
                }
            } catch (OpenRaoImportException e) {
                creationContext.getCreationReport().removed(e.getMessage());
            }
        });
    }

    private void addPreventiveCnec(Branch<?> branch, boolean optimized, boolean monitored) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withNetworkElement(branch.getId())
            .withId(branch.getNameOrId() + "_" + crac.getPreventiveInstant().getId())
            .withInstant(crac.getPreventiveInstant().getId())
            .withOptimized(optimized)
            .withMonitored(monitored);
        cracCreationParameters.getDefaultMonitoredSides().forEach(
            side -> {
                adder.withNominalVoltage(branch.getTerminal(side).getVoltageLevel().getNominalV(), side);
                addThresholdFromPermLimit(adder, branch, side, crac.getPreventiveInstant());
            }
        );
        try {
            adder.add();
        } catch (OpenRaoException e) {
            throw new OpenRaoImportException(ImportStatus.OTHER, String.format("Branch %s (preventive): %s", branch.getId(), e.getMessage()));
        }
    }

    private void addPostContingencyCnec(Branch<?> branch, Contingency contingency, boolean optimized, boolean monitored, Instant instant) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withNetworkElement(branch.getId())
            .withContingency(contingency.getId())
            .withId(branch.getNameOrId() + "_" + contingency.getName().orElse(contingency.getId()) + "_" + instant.getId())
            .withInstant(instant.getId())
            .withOptimized(optimized)
            .withMonitored(monitored);
        cracCreationParameters.getDefaultMonitoredSides().forEach(
            side -> {
                adder.withNominalVoltage(branch.getTerminal(side).getVoltageLevel().getNominalV(), side);
                if (specificParameters.getCriticalElements().getThresholdDefinition() == CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER) {
                    addThresholdFromPermLimit(adder, branch, side, instant);
                } else {
                    addThresholdsFromTempLimit(adder, branch, side, instant);
                }
            }
        );
        try {
            adder.add();
        } catch (OpenRaoException e) {
            throw new OpenRaoImportException(ImportStatus.OTHER, String.format("Branch %s (%s @ %s): %s", branch.getId(), contingency.getName().orElse(contingency.getId()), instant.getId(), e.getMessage()));
        }
    }

    private void addThresholdFromPermLimit(FlowCnecAdder adder, Branch<?> branch, TwoSides side, Instant instant) {
        Optional<OperationalLimitsGroup> optOlg = side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
        if (optOlg.isEmpty()) {
            return;
        }
        OperationalLimitsGroup olg = optOlg.get();
        addThresholdFromPermLimit(adder, side, olg.getCurrentLimits().orElse(null), Unit.AMPERE, branch.getTerminal(side).getVoltageLevel().getNominalV(), instant);
        addThresholdFromPermLimit(adder, side, olg.getActivePowerLimits().orElse(null), Unit.MEGAWATT, branch.getTerminal(side).getVoltageLevel().getNominalV(), instant);
    }

    private void addThresholdsFromTempLimit(FlowCnecAdder adder, Branch<?> branch, TwoSides side, Instant instant) {
        Double duration = specificParameters.getCriticalElements().getApplicableLimitDuration(instant, branch.getTerminal(side).getVoltageLevel().getNominalV());
        if (Double.isInfinite(duration)) {
            addThresholdFromPermLimit(adder, branch, side, instant);
            return;
        }
        Optional<OperationalLimitsGroup> olg = side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
        if (olg.isEmpty()) {
            return;
        }
        addThresholdsFromTempLimit(adder, branch, side, instant, olg.get().getCurrentLimits().orElse(null), duration);
        addThresholdsFromTempLimit(adder, branch, side, instant, olg.get().getActivePowerLimits().orElse(null), duration);
    }

    private void addThresholdsFromTempLimit(FlowCnecAdder adder, Branch<?> branch, TwoSides side, Instant instant, @Nullable LoadingLimits loadingLimits, double acceptableDuration) {
        if (loadingLimits == null) {
            return;
        }
        Optional<LoadingLimits.TemporaryLimit> lowestCurrentLimit = loadingLimits.getTemporaryLimits().stream().filter(tl -> tl.getAcceptableDuration() <= acceptableDuration)
            .filter(tl -> !Double.isNaN(tl.getValue())).min(Comparator.comparingDouble(LoadingLimits.TemporaryLimit::getValue));
        lowestCurrentLimit.ifPresent(tl -> addThresholdFromTempLimit(adder, side, tl, Unit.AMPERE, instant, branch.getTerminal(side).getVoltageLevel().getNominalV()));
    }

    private void addThresholdFromPermLimit(FlowCnecAdder adder, TwoSides side, @Nullable LoadingLimits loadingLimits, Unit unit, Double nominalV, Instant instant) {
        if (loadingLimits == null || Double.isNaN(loadingLimits.getPermanentLimit())) {
            return;
        }
        double limit = specificParameters.getCriticalElements().getLimitMultiplierPerInstant(instant, nominalV) * loadingLimits.getPermanentLimit();
        // TODO also use powsybl limit reductions here
        adder.newThreshold()
            .withSide(side)
            .withMax(limit)
            .withMin(-limit)
            .withUnit(unit)
            .add();
    }

    private void addThresholdFromTempLimit(FlowCnecAdder adder, TwoSides side, LoadingLimits.TemporaryLimit tempLimit, Unit unit, Instant instant, Double nominalV) {
        double limit = specificParameters.getCriticalElements().getLimitMultiplierPerInstant(instant, nominalV) * tempLimit.getValue();
        // TODO also use powsybl limit reductions here
        adder.newThreshold()
            .withSide(side)
            .withMax(limit)
            .withMin(-limit)
            .withUnit(unit)
            .add();
    }

    private void addPstRangeActions() {
        PstRangeActions params = specificParameters.getPstRangeActions();
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
            .filter(params::arePstsAvailableForInstant).collect(Collectors.toSet());
        network.getTwoWindingsTransformerStream()
            .filter(twt -> twt.getPhaseTapChanger() != null)
            .filter(twt -> Utils.branchIsInCountries(twt, params.getCountries().orElse(null)))
            .forEach(twt -> instants.forEach(instant -> addPstRangeActionForInstant(twt, instant)));
    }

    private void addPstRangeActionForInstant(TwoWindingsTransformer twt, Instant instant) {
        PstRangeActions params = specificParameters.getPstRangeActions();
        PstHelper pstHelper = new IidmPstHelper(twt.getId(), network);
        PstRangeActionAdder pstAdder = crac.newPstRangeAction()
            .withId("PST_RA_" + twt.getId() + "_" + instant.getId())
            .withNetworkElement(twt.getId())
            .withInitialTap(pstHelper.getInitialTap())
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(pstHelper.getLowTapPosition()).withMaxTap(pstHelper.getHighTapPosition()).add()
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap());
        // TODO align RAs if needed

        boolean availableForAllStates = crac.getStates(instant).stream().allMatch(state -> params.isAvailable(twt, state));
        if (availableForAllStates) {
            pstAdder.newOnInstantUsageRule().withInstant(instant.getId()).add();
        } else {
            crac.getStates().stream().filter(state -> params.isAvailable(twt, state))
                .forEach(
                    state -> pstAdder.newOnContingencyStateUsageRule()
                        .withInstant(instant.getId())
                        .withContingency(state.getContingency().orElseThrow().getId())
                        .add()
                );
        }
        if (params.getRangeMin(instant).isPresent() || params.getRangeMax(instant).isPresent()) {
            TapRangeAdder rangeAdder = pstAdder.newTapRange().withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT);
            params.getRangeMin(instant).ifPresent(rangeAdder::withMinTap);
            params.getRangeMax(instant).ifPresent(rangeAdder::withMaxTap);
            rangeAdder.add();
        }
        pstAdder.add();
    }

    private void addRedispatchRangeActions() {
        RedispatchingRangeActions params = specificParameters.getRedispatchingRangeActions();
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage()).collect(Collectors.toSet());
        network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, params.getCountries().orElse(null)))
            .forEach(generator ->
                instants.stream().filter(instant -> params.shouldCreateRedispatchingAction(generator, instant))
                    .forEach(instant -> addGeneratorActionForInstant(generator, instant)));
        // TODO add other injections (Loads, batteries...)
    }

    private void addGeneratorActionForInstant(Generator generator, Instant instant) {
        // TODO merge preventive & curative RA if ranges are the same?
        // advantage : simpler crac
        // disadvantage : more complex code + we might not see bugs in "detailed" version
        RedispatchingRangeActions params = specificParameters.getRedispatchingRangeActions();
        double initialP = Math.round(generator.getTargetP());
        // TODO round it in network too ?
        double minP = Math.min(generator.getMinP(), generator.getTargetP());
        if (params.getRaRange(generator, instant).getMin().isPresent()) {
            minP = Math.min(minP, params.getRaRange(generator, instant).getMin().get());
        }
        double maxP = Math.max(generator.getMaxP(), generator.getTargetP());
        if (params.getRaRange(generator, instant).getMax().isPresent()) {
            maxP = Math.max(maxP, params.getRaRange(generator, instant).getMax().get());
        }
        InjectionRangeActionCosts costs = params.getRaCosts(generator, instant);
        crac.newInjectionRangeAction()
            .withId("RD_RA_" + generator.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, generator.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();

        // connect the generator
        generator.connect(SwitchPredicates.IS_OPEN);
    }


}
