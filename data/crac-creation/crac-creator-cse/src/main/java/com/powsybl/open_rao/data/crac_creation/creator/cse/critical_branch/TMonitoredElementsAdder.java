/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.cse.critical_branch;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_creation.creator.cse.*;
import com.powsybl.open_rao.data.crac_creation.creator.cse.xsd.*;
import com.powsybl.open_rao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;

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
        if (tMonitoredElements == null) {
            return;
        }
        importPreventiveMne(tMonitoredElements);
        importCurativeMne(tMonitoredElements);
    }

    private void importPreventiveMne(TMonitoredElements tMonitoredElements) {
        tMonitoredElements.getMonitoredElement().forEach(tMonitoredElement -> {
            if (tMonitoredElement.getBranch().size() == 1) {
                addBaseCaseBranch(List.of(tMonitoredElement.getBranch().get(0)));
            } else {
                addBaseCaseBranch(tMonitoredElement.getBranch());
            }
        });
    }

    private void importCurativeMne(TMonitoredElements tMonitoredElements) {
        if (tMonitoredElements == null || tcracSeries.getOutages().getOutage().isEmpty()) {
            return;
        }
        tMonitoredElements.getMonitoredElement().forEach(tMonitoredElement -> {
            if (tMonitoredElement.getBranch().size() == 1) {
                tcracSeries.getOutages().getOutage().forEach(tOutage -> addBranch(List.of(tMonitoredElement.getBranch().get(0)), tOutage));
            }
        });
    }

    private void addBaseCaseBranch(List<TBranch> tBranches) {
        addBranch(tBranches, null);
    }

    private void addBranch(List<TBranch> tBranches, TOutage tOutage) {
        CriticalBranchReader criticalBranchReader = new CriticalBranchReader(tBranches, tOutage, crac, ucteNetworkAnalyzer, defaultMonitoredSides, true);
        cseCracCreationContext.addMonitoredElementCreationContext(new CseCriticalBranchCreationContext(criticalBranchReader));
    }
}
