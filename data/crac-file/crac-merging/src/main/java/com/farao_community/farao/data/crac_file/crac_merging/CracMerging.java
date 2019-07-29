/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.crac_merging;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CRAC merging function
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracMerging {

    private CracMerging() {
        throw new AssertionError("Utility class should not be instanced!");
    }

    private static void addMonitoredBranch(List<MonitoredBranch> list, MonitoredBranch monitoredBranch) {
        if (!list.contains(monitoredBranch)) {
            list.add(monitoredBranch);
        }
    }

    private static void addContingency(List<Contingency> list, Contingency contingency) {

        List<MonitoredBranch> contingencyMonitoredBranches  = new ArrayList<>();

        if (!list.contains(contingency)) {
            list.add(contingency);
        } else {
            int indexCont = list.indexOf(contingency);
            list.get(indexCont).getMonitoredBranches().forEach(mbFromList -> addMonitoredBranch(contingencyMonitoredBranches, mbFromList));
            list.remove(indexCont);
            contingency.getMonitoredBranches().forEach(monitoredBranch -> addMonitoredBranch(contingencyMonitoredBranches, monitoredBranch));
            list.add(indexCont, Contingency.builder().id(contingency.getId()).name(contingency.getName()).contingencyElements(contingency.getContingencyElements()).monitoredBranches(contingencyMonitoredBranches).build());
        }
    }

    private static void addRemedialAction(List<RemedialAction> list, RemedialAction remedialAction) {
        if (!list.contains(remedialAction)) {
            list.add(remedialAction);
        }
    }

    private static String getMergeFormat(List<CracFile> cracFileList) {
        String referenceFormat = cracFileList.get(0).getSourceFormat();
        return cracFileList.stream().anyMatch(cracFile -> !cracFile.getSourceFormat().equals(referenceFormat)) ? "Hybrid format" : referenceFormat;
    }

    private static String getMergeDescription(List<CracFile> cracFileList) {
        if (cracFileList.stream().allMatch(cracFile -> cracFile.getDescription().isEmpty())) {
            return "Merged CRAC.";
        } else {
            return "Merged CRAC: "
                + System.lineSeparator()
                + cracFileList.stream()
                .map(cracFile -> cracFile.getName() + ": " + (cracFile.getDescription().isEmpty() ? "None" : cracFile.getDescription()))
                .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static CracFile merge(List<CracFile> cracFileList, String id, String name) {

        if (cracFileList.isEmpty()) {
            throw new FaraoException("No CRAC file in input!");
        } else if (cracFileList.size() == 1) {
            return cracFileList.get(0);
        }

        List<Contingency> contingenciesResult = new ArrayList<>();
        List<RemedialAction> remedialActionsResult = new ArrayList<>();
        List<MonitoredBranch> preContingencyMonitoredBranches = new ArrayList<>();

        cracFileList.stream().forEach(cracFile -> {

            // copy all Monitored Branches, except if they are perfectly the same
            cracFile.getPreContingency().getMonitoredBranches().forEach(monitoredBranch -> addMonitoredBranch(preContingencyMonitoredBranches, monitoredBranch));

            // copy all Contingencies, and put together those having the same (id, name, List<ContingencyElement>)
            cracFile.getContingencies().forEach(contingency -> addContingency(contingenciesResult, contingency));

            // copy all Remedial Actions, except if they are perfectly the same
            cracFile.getRemedialActions().forEach(remedialAction -> addRemedialAction(remedialActionsResult, remedialAction));
        });

        // finalize merged CRAC
        return CracFile.builder().id(id)
            .name(name)
            .sourceFormat(getMergeFormat(cracFileList))
            .description(getMergeDescription(cracFileList))
            .preContingency(PreContingency.builder().monitoredBranches(preContingencyMonitoredBranches).build())
            .contingencies(contingenciesResult)
            .remedialActions(remedialActionsResult).build();

    }
}
