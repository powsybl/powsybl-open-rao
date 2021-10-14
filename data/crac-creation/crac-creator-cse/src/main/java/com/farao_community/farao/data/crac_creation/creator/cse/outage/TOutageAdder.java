/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.outage;

import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cse.*;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TBranch;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TCRACSeries;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TOutage;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TOutages;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteContingencyElementHelper;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class TOutageAdder {

    private final TCRACSeries tcracSeries;
    private final Crac crac;
    private final UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private final CseCracCreationContext cseCracCreationContext;

    public TOutageAdder(TCRACSeries tcracSeries, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, CseCracCreationContext cseCracCreationContext) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
    }

    public void add() {
        TOutages tOutages = tcracSeries.getOutages();
        if (tOutages != null) {
            tOutages.getOutage().forEach(tOutage -> {
                if (tOutage != null) {
                    importContingency(tOutage);
                }
            });
        }
    }

    private void importContingency(TOutage tOutage) {
        String outageId = tOutage.getName().getV();
        ContingencyAdder contingencyAdder = crac.newContingency().withId(outageId);
        List<UcteContingencyElementHelper> contingencyElementHelpers = new ArrayList<>();
        tOutage.getBranch().forEach(tBranch -> handleTBranch(tBranch, contingencyAdder, contingencyElementHelpers, outageId));
        if (atLeastOneBranchIsMissing(contingencyElementHelpers)) {
            addNotAddedOutageCreationContext(tOutage, contingencyElementHelpers);
            crac.removeContingency(outageId);
        } else {
            contingencyAdder.add();
            cseCracCreationContext.addOutageCreationContext(CseOutageCreationContext.imported(tOutage.getName().getV()));
        }
    }

    private void addNotAddedOutageCreationContext(TOutage tOutage, List<UcteContingencyElementHelper> branchHelpers) {
        branchHelpers.stream().filter(branchHelper -> !branchHelper.isValid()).forEach(ucteContingencyElementHelper ->
            cseCracCreationContext.addOutageCreationContext(CseOutageCreationContext.notImported(tOutage.getName().getV(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, ucteContingencyElementHelper.getInvalidReason()))
        );
    }

    private boolean atLeastOneBranchIsMissing(List<UcteContingencyElementHelper> branchHelpers) {
        return branchHelpers.stream().anyMatch(branchHelper -> !branchHelper.isValid());
    }

    private void handleTBranch(TBranch tBranch, ContingencyAdder contingencyAdder, List<UcteContingencyElementHelper> branchHelpers, String outageId) {
        UcteContingencyElementHelper branchHelper = new UcteContingencyElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), outageId, ucteNetworkAnalyzer);
        if (!branchHelper.isValid()) {
            branchHelpers.add(branchHelper);
        } else {
            contingencyAdder.withNetworkElement(branchHelper.getIdInNetwork()).add();
        }
    }
}
