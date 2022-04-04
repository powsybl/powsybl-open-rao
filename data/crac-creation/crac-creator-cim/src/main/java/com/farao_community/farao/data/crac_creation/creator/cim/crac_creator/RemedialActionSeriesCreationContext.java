/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public final class RemedialActionSeriesCreationContext implements ElementaryCreationContext {
    private final String remedialActionSeriesId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final boolean isAltered;

    private RemedialActionSeriesCreationContext(String remedialActionSeriesId, ImportStatus importStatus, boolean isAltered, String importStatusDetail) {
        this.remedialActionSeriesId = remedialActionSeriesId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    static RemedialActionSeriesCreationContext notImported(String remedialActionSeriesId, ImportStatus importStatus, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(remedialActionSeriesId, importStatus, false, importStatusDetail);
    }

    static RemedialActionSeriesCreationContext imported(String remedialActionSeriesId, boolean isAltered, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(remedialActionSeriesId, ImportStatus.IMPORTED, isAltered, importStatusDetail);
    }

    public String getNativeId() {
        return remedialActionSeriesId;
    }

    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public String getImportStatusDetail() {
        return importStatusDetail;
    }

    public boolean isAltered() {
        return isAltered;
    }
}

