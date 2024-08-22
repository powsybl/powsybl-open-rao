/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.outage;

import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.cse.CseCracCreationContext;
import com.powsybl.openrao.data.cracio.cse.xsd.TBranch;
import com.powsybl.openrao.data.cracio.cse.xsd.TCRACSeries;
import com.powsybl.openrao.data.cracio.cse.xsd.TOutage;
import com.powsybl.openrao.data.cracio.cse.xsd.TOutages;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteContingencyElementHelper;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteNetworkAnalyzer;

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
        } else {
            contingencyAdder.add();
            cseCracCreationContext.addOutageCreationContext(StandardElementaryCreationContext.imported(tOutage.getName().getV(), tOutage.getName().getV(), tOutage.getName().getV(), false, null));
        }
    }

    private void addNotAddedOutageCreationContext(TOutage tOutage, List<UcteContingencyElementHelper> branchHelpers) {
        branchHelpers.stream().filter(branchHelper -> !branchHelper.isValid()).forEach(ucteContingencyElementHelper ->
            cseCracCreationContext.addOutageCreationContext(StandardElementaryCreationContext.notImported(tOutage.getName().getV(), tOutage.getName().getV(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, ucteContingencyElementHelper.getInvalidReason()))
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
            contingencyAdder.withContingencyElement(branchHelper.getIdInNetwork(), branchHelper.getContingencyTypeInNetwork());
        }
    }
}
