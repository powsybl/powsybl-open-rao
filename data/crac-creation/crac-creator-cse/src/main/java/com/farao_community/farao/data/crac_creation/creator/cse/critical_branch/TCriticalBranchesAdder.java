/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.critical_branch;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.*;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.*;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;

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

    public TCriticalBranchesAdder(TCRACSeries tcracSeries, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, CseCracCreationContext cseCracCreationContext) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
    }

    public void add() {
        TCriticalBranches tCriticalBranches = tcracSeries.getCriticalBranches();
        if (tCriticalBranches != null) {
            importPreventiveCnecs(tCriticalBranches);
            importCurativeCnecs(tCriticalBranches);
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
        CriticalBranchReader criticalBranchReader = new CriticalBranchReader(tBranch, tOutage, crac, ucteNetworkAnalyzer);
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
