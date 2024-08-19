/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.criticalbranch;

import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CseCriticalBranchCreationContext extends StandardElementaryCreationContext implements BranchCnecCreationContext {
    private final NativeBranch nativeBranch;
    private final boolean isBaseCase;
    private final String contingencyId;
    private final Map<String, String> createdCnecIds;
    private final boolean isDirectionInverted;
    private final boolean selected;

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
    public boolean isDirectionInvertedInNetwork() {
        return isDirectionInverted;
    }

    @Override
    public String getCreatedObjectId() {
        throw new NotImplementedException("Several objects may have been created. Please use getCreatedCnecsIds() instead.");
    }

    @Override
    public Set<String> getCreatedObjectsIds() {
        return new HashSet<>(createdCnecIds.values());
    }

    @Override
    public Map<String, String> getCreatedCnecsIds() {
        return createdCnecIds;
    }

    public boolean isSelected() {
        return selected;
    }

    CseCriticalBranchCreationContext(CriticalBranchReader criticalBranchReader) {
        super(criticalBranchReader.getCriticalBranchName(), null, null, criticalBranchReader.getImportStatus(), criticalBranchReader.getInvalidBranchReason(), false);
        this.nativeBranch = criticalBranchReader.getNativeBranch();
        this.isBaseCase = criticalBranchReader.isBaseCase();
        this.createdCnecIds = criticalBranchReader.getCreatedCnecIds();
        this.contingencyId = criticalBranchReader.getContingencyId();
        this.isDirectionInverted = criticalBranchReader.isDirectionInverted();
        this.selected = criticalBranchReader.isSelected();
    }
}
