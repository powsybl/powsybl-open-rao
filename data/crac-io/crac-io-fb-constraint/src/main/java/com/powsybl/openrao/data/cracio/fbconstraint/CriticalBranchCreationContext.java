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
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.NativeBranch;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class CriticalBranchCreationContext implements BranchCnecCreationContext {

    private final String criticalBranchId;
    private final NativeBranch nativeBranch;
    private final boolean isBaseCase;
    private final boolean isImported;
    private final Map<String, String> createdCnecIds;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final String contingencyId;
    private final boolean isDirectionInverted;

    @Override
    public String getNativeObjectId() {
        return criticalBranchId;
    }

    @Override
    public Optional<String> getNativeObjectName() {
        return Optional.empty();
    }

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
    public boolean isImported() {
        return isImported;
    }

    @Override
    public boolean isAltered() {
        return false;
    }

    @Override
    public ImportStatus getImportStatus() {
        return importStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return importStatusDetail;
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

    CriticalBranchCreationContext(CriticalBranchReader criticalBranchReader, Crac crac) {
        this.criticalBranchId = criticalBranchReader.getCriticalBranch().getId();
        this.nativeBranch = criticalBranchReader.getNativeBranch();
        this.isBaseCase = criticalBranchReader.isBaseCase();
        this.isImported = criticalBranchReader.isCriticialBranchValid();
        this.createdCnecIds = new HashMap<>();
        this.importStatus = criticalBranchReader.getImportStatus();
        this.importStatusDetail = criticalBranchReader.getImportStatusDetail();

        if (criticalBranchReader.isCriticialBranchValid() && criticalBranchReader.isBaseCase()) {
            this.isDirectionInverted = criticalBranchReader.isInvertedInNetwork();
            this.createdCnecIds.put(crac.getPreventiveInstant().getId(), criticalBranchReader.getBaseCaseCnecId());
            this.contingencyId = null;
        } else if (criticalBranchReader.isCriticialBranchValid() && !criticalBranchReader.isBaseCase()) {
            this.isDirectionInverted = criticalBranchReader.isInvertedInNetwork();
            this.createdCnecIds.put(crac.getOutageInstant().getId(), criticalBranchReader.getOutageCnecId());
            this.createdCnecIds.put(crac.getInstant(InstantKind.CURATIVE).getId(), criticalBranchReader.getCurativeCnecId());
            this.contingencyId = criticalBranchReader.getOutageReader().getOutage().getId();
        } else {
            this.contingencyId = null;
            this.isDirectionInverted = false;
        }
    }

    private CriticalBranchCreationContext(String criticalBranchId, NativeBranch nativeBranch, boolean isBaseCase, String contingencyId, boolean isImported, Map<String, String> createdCnecIds, boolean isDirectionInverted, ImportStatus importStatus, String importStatusDetail) {
        this.criticalBranchId = criticalBranchId;
        this.nativeBranch = nativeBranch;
        this.isBaseCase = isBaseCase;
        this.contingencyId = contingencyId;
        this.isImported = isImported;
        this.createdCnecIds = createdCnecIds;
        this.isDirectionInverted = isDirectionInverted;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
    }

    public static CriticalBranchCreationContext notImported(String criticalBranchId, ImportStatus importStatus, String importStatusDetail) {
        return new CriticalBranchCreationContext(criticalBranchId, null, false, null, false, new HashMap<>(), false, importStatus, importStatusDetail);
    }
}
