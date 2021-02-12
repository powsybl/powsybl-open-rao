/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_util.CracCleaningFeature.CHECK_CNEC_MNEC;
import static com.farao_community.farao.data.crac_util.CracCleaningFeature.REMOVE_UNHANDLED_CONTINGENCIES;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class CracCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracCleaner.class);

    public CracCleaner() {
    }

    public List<String> cleanCrac(Crac crac, Network network) {
        List<String> report = new ArrayList<>();

        // remove Cnec whose NetworkElement is absent from the network
        ArrayList<BranchCnec> absentFromNetworkCnecs = new ArrayList<>();
        crac.getBranchCnecs().forEach(cnec -> {
            if (network.getBranch(cnec.getNetworkElement().getId()) == null) {
                absentFromNetworkCnecs.add(cnec);
                report.add(String.format("[REMOVED] Cnec %s with network element [%s] is not present in the network. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        absentFromNetworkCnecs.forEach(cnec -> crac.removeCnec(cnec.getId()));

        if (CHECK_CNEC_MNEC.isEnabled()) {
            // remove Cnecs that are neither optimized nor monitored
            ArrayList<BranchCnec> unmonitoredCnecs = new ArrayList<>();
            crac.getBranchCnecs().forEach(cnec -> {
                if (!cnec.isOptimized() && !cnec.isMonitored()) {
                    unmonitoredCnecs.add(cnec);
                    report.add(String.format("[REMOVED] Cnec %s with network element [%s] is neither optimized nor monitored. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
                }
            });
            unmonitoredCnecs.forEach(cnec -> crac.removeCnec(cnec.getId()));
        }

        // remove RangeAction whose NetworkElement is absent from the network
        ArrayList<RangeAction> absentFromNetworkRangeActions = new ArrayList<>();
        for (RangeAction rangeAction: crac.getRangeActions()) {
            rangeAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkRangeActions.add(rangeAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", rangeAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkRangeActions.forEach(rangeAction -> crac.removeRangeAction(rangeAction.getId()));

        // remove NetworkAction whose NetworkElement is absent from the network
        ArrayList<NetworkAction> absentFromNetworkNetworkActions = new ArrayList<>();
        for (NetworkAction networkAction: crac.getNetworkActions()) {
            networkAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkNetworkActions.add(networkAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", networkAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkNetworkActions.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        // list Contingencies whose NetworkElement is absent from the network or does not fit a valid Powsybl Contingency
        Set<Contingency> removedContingencies = new HashSet<>();
        for (Contingency contingency : crac.getContingencies()) {
            if (!contingency.isSynchronized()) {
                // It is necessary to skip XnodeContingencies that have not been synchronized since we don't know their
                // network elements yet
                // However, this means that we cannot clean out wrong XnodeContingencies
                // When Crac import is refactored, we should be able to access the network at the time of creation
                // of the contingencies. Then the network elements can be found from the start
                // TODO : remove this when CRAC importer can use network
                continue;
            }
            contingency.getNetworkElements().forEach(networkElement -> {
                Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
                if (identifiable == null) {
                    removedContingencies.add(contingency);
                    report.add(String.format("[REMOVED] Contingency %s with network element [%s] is not present in the network. It is removed from the Crac", contingency.getId(), networkElement.getId()));
                } else if (!(identifiable instanceof Branch || identifiable instanceof Generator || identifiable instanceof HvdcLine || identifiable instanceof BusbarSection || identifiable instanceof DanglingLine)) {
                    if (REMOVE_UNHANDLED_CONTINGENCIES.isEnabled()) {
                        removedContingencies.add(contingency);
                        report.add(String.format("[REMOVED] Contingency %s has a network element [%s] of unhandled type [%s].  It is removed from the Crac.", contingency.getId(), networkElement.getId(), identifiable.getClass().toString()));
                    } else {
                        report.add(String.format("[WARNING] Contingency %s has a network element [%s] of unhandled type [%s]. This may result in unexpected behavior.", contingency.getId(), networkElement.getId(), identifiable.getClass().toString()));
                    }
                    // do not delete contingencies now as they are needed to check which associated states/cnecs should be removed as well
                }
            });
        }

        // list States whose contingency does not exist anymore
        Set<State> removedStates = new HashSet<>();
        for (Contingency contingency : removedContingencies) {
            crac.getStates(contingency).forEach(state -> {
                report.add(String.format("[REMOVED] State %s is removed because its associated contingency [%s] has been removed", state.getId(), contingency.getId()));
                removedStates.add(state);
            });
            // do not delete states now as they are needed to check which associated ra/cnecs should be removed as well
        }

        // remove Cnec whose contingency does not exist anymore
        removedContingencies.forEach(contingency ->
            crac.getStatesFromContingency(contingency.getId()).forEach(state ->
                crac.getBranchCnecs(state).forEach(cnec -> {
                    crac.removeCnec(cnec.getId());
                    report.add(String.format("[REMOVED] Cnec %s is removed because its associated contingency [%s] has been removed", cnec.getId(), contingency.getId()));
                })
            )
        );

        // remove Remedial Action with an empty list of NetworkElement
        Set<RemedialAction<?>> noValidAction = new HashSet<>();
        crac.getNetworkActions().stream().filter(na -> na.getNetworkElements().isEmpty()).forEach(na -> {
            report.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", na.getId()));
            noValidAction.add(na);
        });
        crac.getRangeActions().stream().filter(ra -> ra.getNetworkElements().isEmpty()).forEach(ra -> {
            report.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", ra.getId()));
            noValidAction.add(ra);
        });
        noValidAction.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        // remove On State usage rule with invalid state
        crac.getRangeActions().forEach(ra -> cleanUsageRules(ra, removedStates, report));
        crac.getNetworkActions().forEach(ra -> cleanUsageRules(ra, removedStates, report));

         /* TODO : remove range actions with initial setpoints that do not respect their authorized range
         This is not possible now since, for PstRange, we have to synchronize them first in order
         to be able to access current / min / max setpoints
         We can do this during CRAC refactoring (we should somehow merge CracCleaner.cleanCrac() & crac.synchronize() methods)
         For now, these "wrong" range actions are only handled in the LinearOptimizer (in the CoreProblemFiller)*/

        // remove states and contingencies
        removedContingencies.forEach(contingency -> crac.removeContingency(contingency.getId()));
        removedStates.forEach(state -> crac.removeState(state.getId()));
        report.forEach(LOGGER::warn);

        // remove PstRanges with a RELATIVE_TO_PREVIOUS_INSTANT for preventive PST RAs
        List<RangeAction> removedRangeActions = new ArrayList<>();
        crac.getRangeActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE).stream().filter(ra -> ra.getUsageRules().size() == 1).forEach(ra -> {
            List<Range> removedRanges = new ArrayList<>();
            ra.getRanges().forEach(range -> {
                if (range.getRangeType() == RangeType.RELATIVE_TO_PREVIOUS_INSTANT) {
                    report.add(String.format("[REMOVED] Range Action %s is a preventive range action with a range relative to previous instant. That range has been removed.", ra.getId()));
                    removedRanges.add(range);
                }
            });
            removedRanges.forEach(ra::removeRange);
            if (ra.getRanges().isEmpty()) {
                report.add(String.format("[REMOVED] Range Action %s has no ranges. It has been removed from the Crac.", ra.getId()));
                removedRangeActions.add(ra);
            }
        });
        removedRangeActions.forEach(ra -> crac.removeRangeAction(ra.getId()));

        return report;
    }

    private static void cleanUsageRules(RemedialAction<?> remedialAction, Set<State> removedStates, List<String> report) {
        Set<UsageRule> removedUr = new HashSet<>();
        remedialAction.getUsageRules().forEach(usageRule -> {
            if (usageRule instanceof OnState && removedStates.contains(((OnState) usageRule).getState())) {
                report.add(String.format("[REMOVED] OnState usage rule of RA %s is removed because its associated state [%s] has been removed",
                    remedialAction.getId(),
                    ((OnState) usageRule).getState().getId()));
                removedUr.add(usageRule);
            }
        });
        remedialAction.getUsageRules().removeAll(removedUr);
    }

    public void enableFeature(CracCleaningFeature feature) {
        feature.enable();
    }

    public void disableFeature(CracCleaningFeature feature) {
        feature.disable();
    }
}
