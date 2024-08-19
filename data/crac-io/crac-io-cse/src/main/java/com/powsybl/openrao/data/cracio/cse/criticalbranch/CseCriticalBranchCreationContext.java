/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.criticalbranch;

import com.powsybl.openrao.data.cracio.commons.api.StandardCriticalBranchCreationContext;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CseCriticalBranchCreationContext extends StandardCriticalBranchCreationContext {
    private final boolean selected;

    public boolean isSelected() {
        return selected;
    }

    CseCriticalBranchCreationContext(CriticalBranchReader criticalBranchReader) {
        super(criticalBranchReader.getCriticalBranchName(), criticalBranchReader.getNativeBranch(), criticalBranchReader.isBaseCase(), criticalBranchReader.getContingencyId(), criticalBranchReader.getCreatedCnecIds(), criticalBranchReader.isDirectionInverted(), criticalBranchReader.getImportStatus(), criticalBranchReader.getInvalidBranchReason());
        this.selected = criticalBranchReader.isSelected();
    }
}
