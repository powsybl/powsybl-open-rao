/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.criticalbranch;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.cracio.cse.CseCracCreationContext;
import com.powsybl.openrao.data.cracio.cse.xsd.TBaseCaseBranches;
import com.powsybl.openrao.data.cracio.cse.xsd.TBranch;
import com.powsybl.openrao.data.cracio.cse.xsd.TCRACSeries;
import com.powsybl.openrao.data.cracio.cse.xsd.TCriticalBranches;
import com.powsybl.openrao.data.cracio.cse.xsd.TOutage;

import java.util.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class TCriticalBranchesAdder {
    private final TCRACSeries tcracSeries;
    private final Crac crac;
    private final UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private final CseCracCreationContext cseCracCreationContext;
    private final Map<String, Set<String>> remedialActionsForCnecsMap = new HashMap<>(); // contains for each RA the set of CNEC IDs for which it can be activated
    private final Set<TwoSides> defaultMonitoredSides;

    public TCriticalBranchesAdder(TCRACSeries tcracSeries, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, CseCracCreationContext cseCracCreationContext, Set<TwoSides> defaultMonitoredSides) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
    }

    public void add() {
        TCriticalBranches tCriticalBranches = tcracSeries.getCriticalBranches();
        if (tCriticalBranches != null) {
            importCurativeCnecs(tCriticalBranches);
            importPreventiveCnecs(tCriticalBranches);
        }
    }

    private void importPreventiveCnecs(TCriticalBranches tCriticalBranches) {
        TBaseCaseBranches tBaseCaseBranches = tCriticalBranches.getBaseCaseBranches();
        if (tBaseCaseBranches != null) {
            tBaseCaseBranches.getBranch().forEach(this::addBaseCaseBranch);
        }
    }

    private void importCurativeCnecs(TCriticalBranches tCriticalBranches) {
        tCriticalBranches.getCriticalBranch().forEach(tCriticalBranch ->
                tCriticalBranch.getBranch().forEach(tBranch ->
                        addBranch(tBranch, tCriticalBranch.getOutage())));
    }

    private void addBaseCaseBranch(TBranch tBranch) {
        addBranch(tBranch, null);
    }

    private void addBranch(TBranch tBranch, TOutage tOutage) {
        CriticalBranchReader criticalBranchReader = new CriticalBranchReader(List.of(tBranch), tOutage, crac, ucteNetworkAnalyzer, defaultMonitoredSides, false);
        cseCracCreationContext.addCriticalBranchCreationContext(new CseCriticalBranchCreationContext(criticalBranchReader));
        addRemedialActionsForCnecs(criticalBranchReader.getCreatedCnecIds().values(), criticalBranchReader.getRemedialActionIds());
    }

    private void addRemedialActionsForCnecs(Collection<String> cnecIds, Set<String> remedialActionIds) {
        for (String remedialActionId : remedialActionIds) {
            remedialActionsForCnecsMap.putIfAbsent(remedialActionId, new HashSet<>());
            remedialActionsForCnecsMap.get(remedialActionId).addAll(cnecIds);
        }
    }

    public Map<String, Set<String>> getRemedialActionsForCnecsMap() {
        return remedialActionsForCnecsMap;
    }
}
