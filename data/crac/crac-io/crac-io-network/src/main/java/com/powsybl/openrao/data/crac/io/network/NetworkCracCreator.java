/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;


import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreator {
    private NetworkCracCreationContext creationContext;
    private CracCreationParameters genericParameters;
    private NetworkCracCreationParameters parameters;
    private Network network;
    private Crac crac;
    private Set<Branch<?>> optimizedBranches;
    private Set<Branch<?>> monitoredBranches;
    private Set<Branch<?>> criticalBranches;

    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    NetworkCracCreationContext createCrac(Network network, CracCreationParameters cracCreationParameters) {
        this.network = network;

        this.creationContext = new NetworkCracCreationContext(crac, network.getNameOrId());

        if (cracCreationParameters.getExtension(NetworkCracCreationParameters.class) == null) {
            throw new OpenRaoException("Cannot create a CRAC from a network file unless a NetworkCracCreationParameters extension is defined in CracCreationParameters.");
        }
        this.genericParameters = cracCreationParameters;
        this.parameters = cracCreationParameters.getExtension(NetworkCracCreationParameters.class);

        String cracId = "CRAC_" + network.getNameOrId();
        this.crac = cracCreationParameters.getCracFactory().create(cracId, cracId, network.getCaseDate().toOffsetDateTime());
        getCriticalBranches();
        addInstants(); // TODO figure out what instants to create
        addOptimizedCnecs();
        addMonitoredCnecs();

        return this.creationContext;
    }

    private void getCriticalBranches() {
        NetworkCracCreationParameters.CriticalBranches params = parameters.criticalBranches();
        optimizedBranches = network.getBranchStream()
            .filter(b ->
                Utils.branchIsInCountries(b, params.countries())
                    && Utils.branchIsInVRange(b, params.optimizedMinV(), params.optimizedMaxV())
                    && ((genericParameters.getDefaultMonitoredSides().contains(TwoSides.ONE) && b.getSelectedOperationalLimitsGroup1().isPresent()) || (genericParameters.getDefaultMonitoredSides().contains(TwoSides.TWO) && b.getSelectedOperationalLimitsGroup2().isPresent()))
            )
            .collect(Collectors.toSet());
        if (params.monitorOtherBranches()) {
            monitoredBranches = network.getBranchStream()
                .filter(b ->
                    Utils.branchIsInCountries(b, params.countries())
                        && !Utils.branchIsInVRange(b, params.optimizedMinV(), params.optimizedMaxV())
                )
                .collect(Collectors.toSet());
        } else {
            monitoredBranches = new HashSet<>();
        }
        criticalBranches = new HashSet<>(optimizedBranches);
        criticalBranches.addAll(monitoredBranches);
    }

    private void addInstants() {
        crac.newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        //if (shouldAddOutageInstant()) {
        crac.newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE);
        //}
        crac.newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        // TODO how to detect how many curative instants, etc ?
    }


    /**
     * Returns true if any critical branch has a current or P limit with a small enough acceptable duration
     */
    private boolean shouldAddOutageInstant() {
        return criticalBranches.stream().anyMatch(
            b -> genericParameters.getDefaultMonitoredSides().stream().anyMatch(side -> requiresOutageInstant(b, side))
        );
    }

    /**
     * Returns true if branch has a current or active power limit, with an acceptable duration inferior to the
     * "outage instant" defined to the user for its given voltage level ; which is the duration under which no RA is possible.
     */
    private boolean requiresOutageInstant(Branch<?> branch, TwoSides side) {
        NetworkCracCreationParameters.CriticalBranches params = parameters.criticalBranches();
        int outageInstant = params.outageInstantPerVoltageLevel().getOrDefault(branch.getTerminal(side).getVoltageLevel(),
            params.outageInstantPerVoltageLevel().values().stream().max(Integer::compare).orElseThrow());
        // TODO map might not contain VL exactly, find closest VL ?
        // currently defaulting to max value
        Optional<OperationalLimitsGroup> olg = side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
        if (olg.isEmpty()) {
            return false;
        }
        Set<LoadingLimits.TemporaryLimit> consideredTempLimits = new HashSet<>();
        if (olg.get().getCurrentLimits().isPresent()) {
            consideredTempLimits.addAll(olg.get().getCurrentLimits().get().getTemporaryLimits());
        }
        if (olg.get().getActivePowerLimits().isPresent()) {
            consideredTempLimits.addAll(olg.get().getActivePowerLimits().get().getTemporaryLimits());
        }
        return consideredTempLimits.stream().anyMatch(
            tl -> tl.getAcceptableDuration() <= outageInstant
        );
    }

    private void addOptimizedCnecs() {
        optimizedBranches.forEach(branch -> {
            try {
                addPreventiveCnec(branch, true, false);
            } catch (OpenRaoImportException e) {
                creationContext.getCreationReport().removed(e.getMessage());
            }
        });
    }

    private void addMonitoredCnecs() {
        monitoredBranches.forEach(branch -> {
            try {
                addPreventiveCnec(branch, false, true);
            } catch (OpenRaoImportException e) {
                creationContext.getCreationReport().removed(e.getMessage());
            }
        });
    }

    private void addPreventiveCnec(Branch<?> branch, boolean optimized, boolean monitored) {
        if (genericParameters.getDefaultMonitoredSides().stream().noneMatch(
            side -> hasAnyPermanentLimit(branch, side)
        )) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Branch %s has no non-NaN current or active power limit", branch.getId()));
        }
        FlowCnecAdder adder = crac.newFlowCnec()
            .withNetworkElement(branch.getId())
            .withId(branch.getId() + "_PREVENTIVE")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOptimized(optimized)
            .withMonitored(monitored);
        genericParameters.getDefaultMonitoredSides().forEach(
            side -> {
                adder.withNominalVoltage(branch.getTerminal(side).getVoltageLevel().getNominalV(), TwoSides.ONE);
                addPreventiveThreshold(adder, branch, side);
            }
        );
        try {
            adder.add();
        } catch (OpenRaoException e) {
            throw new OpenRaoImportException(ImportStatus.OTHER, String.format("Branch %s: %s", branch.getId(), e.getMessage()));
        }
    }

    private boolean hasAnyPermanentLimit(Branch<?> branch, TwoSides side) {
        Optional<OperationalLimitsGroup> olg = side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
        if (olg.isEmpty()) {
            return false;
        }
        return (olg.get().getCurrentLimits().isPresent() && !Double.isNaN(olg.get().getCurrentLimits().get().getPermanentLimit()))
            || (olg.get().getActivePowerLimits().isPresent() && !Double.isNaN(olg.get().getActivePowerLimits().get().getPermanentLimit()));
    }

    private void addPreventiveThreshold(FlowCnecAdder adder, Branch<?> branch, TwoSides side) {
        Optional<OperationalLimitsGroup> optOlg = side == TwoSides.ONE ? branch.getSelectedOperationalLimitsGroup1() : branch.getSelectedOperationalLimitsGroup2();
        if (optOlg.isEmpty()) {
            return;
        }
        OperationalLimitsGroup olg = optOlg.get();
        if (olg.getCurrentLimits().isPresent()) {
            addPreventiveThresholdFromLimits(adder, side, olg.getCurrentLimits().get(), Unit.AMPERE);
        }
        if (olg.getActivePowerLimits().isPresent()) {
            addPreventiveThresholdFromLimits(adder, side, olg.getActivePowerLimits().get(), Unit.MEGAWATT);
        }
    }

    private void addPreventiveThresholdFromLimits(FlowCnecAdder adder, TwoSides side, LoadingLimits loadingLimits, Unit unit) {
        if (Double.isNaN(loadingLimits.getPermanentLimit())) {
            return;
        }
        double limit =
            parameters.criticalBranches().operationalLimitReduction(InstantKind.PREVENTIVE) * loadingLimits.getPermanentLimit();
        // TODO use powsybl limit reductions here
        adder.newThreshold()
            .withSide(side)
            .withMax(limit)
            .withMin(-limit)
            .withUnit(unit);
    }
}
