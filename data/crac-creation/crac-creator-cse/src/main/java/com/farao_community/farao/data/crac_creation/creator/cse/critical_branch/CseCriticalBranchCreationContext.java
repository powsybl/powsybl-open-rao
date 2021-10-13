/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.critical_branch;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.NativeBranch;

import java.util.Map;
import java.util.Optional;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CseCriticalBranchCreationContext implements BranchCnecCreationContext {
    private final String criticalBranchName;
    private final NativeBranch nativeBranch;
    private final boolean isBaseCase;
    private final String contingencyId;
    private final boolean isImported;
    private final Map<Instant, String> createdCnecIds;
    private final boolean isDirectionInverted;
    private final String invalidBranchReason;
    private final ImportStatus criticalBranchImportStatus;

    @Override
    public NativeBranch getNativeBranch() {
        return nativeBranch;
    }

    @Override
    public boolean isBaseCase() {
        return isBaseCase;
    }

    @Override
    public Optional<String> getContingencyId() {
        return isBaseCase ? Optional.empty() : Optional.of(contingencyId);
    }

    @Override
    public String getNativeId() {
        return criticalBranchName;
    }

    @Override
    public boolean isImported() {
        return isImported;
    }

    @Override
    public boolean isAltered() {
        return false;
    }

    @Override
    public ImportStatus getImportStatus() {
        return criticalBranchImportStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return invalidBranchReason;
    }

    @Override
    public boolean isDirectionInvertedInNetwork() {
        return isDirectionInverted;
    }

    @Override
    public Map<Instant, String> getCreatedCnecsIds() {
        return createdCnecIds;
    }

    CseCriticalBranchCreationContext(CriticalBranchReader criticalBranchReader) {
        this.criticalBranchName = criticalBranchReader.getCriticalBranchName();
        this.nativeBranch = criticalBranchReader.getNativeBranch();
        this.isBaseCase = criticalBranchReader.isBaseCase();
        this.isImported = criticalBranchReader.isImported();
        this.createdCnecIds = criticalBranchReader.getCreatedCnecIds();
        this.contingencyId = criticalBranchReader.getContingencyId();
        this.isDirectionInverted = criticalBranchReader.isDirectionInverted();
        this.invalidBranchReason = criticalBranchReader.getInvalidBranchReason();
        this.criticalBranchImportStatus = criticalBranchReader.getImportStatus();
    }
}
