/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AngleCnecCreationContext implements ElementaryCreationContext {
    private String createdAngleCnecId;
    private String contingencyID;
    private String serieId;
    private ImportStatus importStatus;
    private String importStatusDetail;

    private AngleCnecCreationContext(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus importStatus, String importStatusDetail) {
        this.createdAngleCnecId = createdAngleCnecId;
        this.contingencyID = contingencyID;
        this.serieId = serieId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
    }

    static AngleCnecCreationContext notImported(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus importStatus, String importStatusDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, importStatus, importStatusDetail);
    }

    static AngleCnecCreationContext imported(String createdAngleCnecId, String contingencyID, String serieId, String alteringDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, ImportStatus.IMPORTED,  alteringDetail);
    }

    @Override
    public String getNativeId() {
        return createdAngleCnecId;
    }

    @Override
    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
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

    public String getContingencyId() {
        return contingencyID;
    }

    public String getSerieId() {
        return serieId;
    }
}

