/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.powsybl.open_rao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AngleCnecCreationContext implements ElementaryCreationContext {
    private String createdAngleCnecId;
    private String contingencyID;
    private String serieId;
    private ImportStatus angleCnecImportStatus;
    private String angleCnecImportStatusDetail;

    private AngleCnecCreationContext(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus angleCnecImportStatus, String angleCnecImportStatusDetail) {
        this.createdAngleCnecId = createdAngleCnecId;
        this.contingencyID = contingencyID;
        this.serieId = serieId;
        this.angleCnecImportStatus = angleCnecImportStatus;
        this.angleCnecImportStatusDetail = angleCnecImportStatusDetail;
    }

    static AngleCnecCreationContext notImported(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus angleCnecImportStatus, String angleCnecImportStatusDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, angleCnecImportStatus, angleCnecImportStatusDetail);
    }

    static AngleCnecCreationContext imported(String createdAngleCnecId, String contingencyID, String serieId, String alteringDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, ImportStatus.IMPORTED, alteringDetail);
    }

    @Override
    public String getNativeId() {
        return createdAngleCnecId;
    }

    public String getCreatedCnecId() {
        return createdAngleCnecId;
    }

    @Override
    public boolean isImported() {
        return angleCnecImportStatus.equals(ImportStatus.IMPORTED);
    }

    @Override
    public boolean isAltered() {
        return false;
    }

    @Override
    public ImportStatus getImportStatus() {
        return angleCnecImportStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return angleCnecImportStatusDetail;
    }

    public String getContingencyId() {
        return contingencyID;
    }

    public String getSerieId() {
        return serieId;
    }
}

