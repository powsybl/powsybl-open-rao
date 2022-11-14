/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.monitored_elements;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.cse.*;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CriticalBranchReader;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.*;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;

import java.util.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

public class TMonitoredElementsAdder {
    private final TCRACSeries tcracSeries;
    private final Crac crac;
    private final UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private final CseCracCreationContext cseCracCreationContext;
    private final Set<Side> defaultMonitoredSides;

    public TMonitoredElementsAdder(TCRACSeries tcracSeries, Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, CseCracCreationContext cseCracCreationContext, Set<Side> defaultMonitoredSides) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
        this.defaultMonitoredSides = defaultMonitoredSides;
    }

    public void add() {
        TMonitoredElements tMonitoredElements = tcracSeries.getMonitoredElements();
        if (tMonitoredElements != null) {
            importPreventiveMne(tMonitoredElements);
            importCurativeMne(tMonitoredElements);
        }
    }

    private void importPreventiveMne(TMonitoredElements tMonitoredElements) {
        if (tMonitoredElements != null) {
            for (int i = 0; i < tMonitoredElements.getMonitoredElement().size(); i++) {
                if (tMonitoredElements.getMonitoredElement().get(i).getBranch().size() == 1) {
                    addBaseCaseBranch(tMonitoredElements.getMonitoredElement().get(i).getBranch().get(0));
                }
                else {
                    TOutage fakeOutage = new TOutage();
                    TName fakeName = new TName();
                    fakeName.setV("mneHasTooManyBranches");
                    fakeOutage.setName(fakeName);
                    addBranch(tMonitoredElements.getMonitoredElement().get(i).getBranch().get(0), fakeOutage);
                }
            }
        }
    }

    private void importCurativeMne(TMonitoredElements tMonitoredElements) {
        if (tMonitoredElements != null) {
            if (tcracSeries.getOutages().getOutage().size() > 0) {
                for (int i = 0; i < tMonitoredElements.getMonitoredElement().size(); i++) {
                    if (tMonitoredElements.getMonitoredElement().get(i).getBranch().size() == 1) {
                        for (int j = 0; j < tcracSeries.getOutages().getOutage().size(); j++) {
                            addBranch(tMonitoredElements.getMonitoredElement().get(i).getBranch().get(0), tcracSeries.getOutages().getOutage().get(j));
                        }
                    }
                }
            }
        }
    }

    private void addBaseCaseBranch(TBranch tBranch) {
        addBranch(tBranch, null);
    }

    private void addBranch(TBranch tBranch, TOutage tOutage) {
        MonitoredElementReader monitoredElementReader = new MonitoredElementReader(tBranch, tOutage, crac, ucteNetworkAnalyzer, defaultMonitoredSides);
        cseCracCreationContext.addCriticalBranchCreationContext(new CseCriticalBranchCreationContext(monitoredElementReader));
    }
}
