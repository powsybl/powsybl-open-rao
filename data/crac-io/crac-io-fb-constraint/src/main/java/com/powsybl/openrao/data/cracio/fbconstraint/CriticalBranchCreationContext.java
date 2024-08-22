/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardCriticalBranchCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class CriticalBranchCreationContext extends StandardCriticalBranchCreationContext {
    CriticalBranchCreationContext(CriticalBranchReader criticalBranchReader, Crac crac) {
        super(criticalBranchReader.getCriticalBranch().getId(), criticalBranchReader.getNativeBranch(), criticalBranchReader.isBaseCase(), null, new HashMap<>(), criticalBranchReader.isInvertedInNetwork(), criticalBranchReader.getImportStatus(), criticalBranchReader.getImportStatusDetail());
        if (criticalBranchReader.isCriticialBranchValid() && criticalBranchReader.isBaseCase()) {
            this.createdCnecIds.put(crac.getPreventiveInstant().getId(), criticalBranchReader.getBaseCaseCnecId());
        } else if (criticalBranchReader.isCriticialBranchValid() && !criticalBranchReader.isBaseCase()) {
            this.createdCnecIds.put(crac.getOutageInstant().getId(), criticalBranchReader.getOutageCnecId());
            this.createdCnecIds.put(crac.getInstant(InstantKind.CURATIVE).getId(), criticalBranchReader.getCurativeCnecId());
            this.contingencyId = criticalBranchReader.getOutageReader().getOutage().getId();
        } else {
            this.isDirectionInverted = false;
        }
    }

    private CriticalBranchCreationContext(String criticalBranchId, NativeBranch nativeBranch, boolean isBaseCase, String contingencyId, Map<String, String> createdCnecIds, boolean isDirectionInverted, ImportStatus importStatus, String importStatusDetail) {
        super(criticalBranchId, nativeBranch, isBaseCase, contingencyId, createdCnecIds, isDirectionInverted, importStatus, importStatusDetail);
    }

    public static CriticalBranchCreationContext notImported(String criticalBranchId, ImportStatus importStatus, String importStatusDetail) {
        return new CriticalBranchCreationContext(criticalBranchId, null, false, null, new HashMap<>(), false, importStatus, importStatusDetail);
    }
}
