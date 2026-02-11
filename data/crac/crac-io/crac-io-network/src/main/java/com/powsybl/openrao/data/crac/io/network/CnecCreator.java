/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.CriticalElements;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import org.jgrapht.alg.util.Pair;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CnecCreator {
    private final Crac crac;
    private final Network network;
    private final CracCreationParameters cracCreationParameters;
    private final NetworkCracCreationParameters specificParameters;
    private final NetworkCracCreationContext creationContext;

    CnecCreator(NetworkCracCreationContext creationContext, Network network, CracCreationParameters cracCreationParameters) {
        this.creationContext = creationContext;
        this.crac = creationContext.getCrac();
        this.network = network;
        this.cracCreationParameters = cracCreationParameters;
        this.specificParameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);
    }

    void addCnecsAndMnecs() {
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
                } else if (params.getMonitoredMinMaxV().isPresent() && Utils.branchIsInVRange(branch, params.getMonitoredMinMaxV().get().getMin(), params.getMonitoredMinMaxV().get().getMax())) {
                    monitoredBranches.add(branch);
                }
            });
        return Pair.of(optimizedBranches, monitoredBranches);
    }


    private void addCnecs(Set<Branch<?>> branches, boolean optimized) {
        branches.forEach(branch -> {
            try {
                addPreventiveCnec(branch, optimized, !optimized);
                for (Contingency contingency : crac.getContingencies()) {
                    if (!specificParameters.getCriticalElements().shouldCreateCnec(branch, contingency)
                        || contingency.getElements().stream().anyMatch(e -> e.getId().equals(branch.getId()))) {
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
        Optional<LoadingLimits.TemporaryLimit> lowestCurrentLimit = loadingLimits.getTemporaryLimits().stream().filter(tl -> tl.getAcceptableDuration() >= acceptableDuration)
            .filter(tl -> !Double.isNaN(tl.getValue())).max(Comparator.comparingDouble(LoadingLimits.TemporaryLimit::getValue));
        if (lowestCurrentLimit.isPresent()) {
            addThresholdFromTempLimit(adder, side, lowestCurrentLimit.get(), Unit.AMPERE, instant, branch.getTerminal(side).getVoltageLevel().getNominalV());
        } else {
            addThresholdFromPermLimit(adder, branch, side, instant);
        }
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

}
