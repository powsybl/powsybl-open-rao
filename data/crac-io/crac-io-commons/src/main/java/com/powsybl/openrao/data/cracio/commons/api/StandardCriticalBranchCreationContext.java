/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class StandardCriticalBranchCreationContext extends StandardElementaryCreationContext implements BranchCnecCreationContext {
    protected final NativeBranch nativeBranch;
    protected final boolean isBaseCase;
    protected String contingencyId;
    protected final Map<String, String> createdCnecIds;
    protected boolean isDirectionInverted;

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
        return isBaseCase ? Optional.empty() : Optional.ofNullable(contingencyId);
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

    public StandardCriticalBranchCreationContext(String criticalBranchId, NativeBranch nativeBranch, boolean isBaseCase, String contingencyId, Map<String, String> createdCnecIds, boolean isDirectionInverted, ImportStatus importStatus, String importStatusDetail) {
        super(criticalBranchId, null, null, importStatus, importStatusDetail, false);
        this.nativeBranch = nativeBranch;
        this.isBaseCase = isBaseCase;
        this.createdCnecIds = createdCnecIds;
        this.contingencyId = contingencyId;
        this.isDirectionInverted = isDirectionInverted;
    }
}
