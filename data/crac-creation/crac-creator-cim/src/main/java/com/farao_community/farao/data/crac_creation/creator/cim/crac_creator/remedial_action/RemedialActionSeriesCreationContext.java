/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RemedialActionSeriesCreationContext implements ElementaryCreationContext {
    private final String nativeId;
    private final Set<String> createdIds;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final boolean isAltered;
    private final boolean isInverted;

    protected RemedialActionSeriesCreationContext(String nativeId, Set<String> createdIds, ImportStatus importStatus, boolean isAltered, boolean isInverted, String importStatusDetail) {
        this.nativeId = nativeId;
        this.createdIds = createdIds;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
        this.isInverted = isInverted;
    }

    static RemedialActionSeriesCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, Set.of(nativeId), importStatus, false, false, importStatusDetail);
    }

    static RemedialActionSeriesCreationContext imported(String nativeId, boolean isAltered, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, Set.of(nativeId), ImportStatus.IMPORTED, isAltered, false, importStatusDetail);
    }

    static RemedialActionSeriesCreationContext importedHvdcRa(String nativeId, Set<String> createdIds, boolean isAltered, boolean isInverted, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, createdIds, ImportStatus.IMPORTED, isAltered, isInverted, importStatusDetail);
    }

    @Override
    public String getNativeId() {
        return nativeId;
    }

    @Override
    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
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
    public boolean isAltered() {
        return isAltered;
    }

    public boolean isInverted() {
        return isInverted;
    }

    public Set<String> getCreatedIds() {
        return createdIds;
    }
}

