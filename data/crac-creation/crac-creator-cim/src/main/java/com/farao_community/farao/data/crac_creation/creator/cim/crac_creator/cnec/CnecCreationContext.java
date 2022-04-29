/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public final class CnecCreationContext {
    private final String createdCnecId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final boolean isDirectionInverted;

    private CnecCreationContext(String createdCnecId, boolean isDirectionInverted, ImportStatus importStatus, String importStatusDetail) {
        this.createdCnecId = createdCnecId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isDirectionInverted = isDirectionInverted;
    }

    static CnecCreationContext notImported(ImportStatus importStatus, String importStatusDetail) {
        return new CnecCreationContext(null, false, importStatus, importStatusDetail);
    }

    static CnecCreationContext imported(String createdCnecId, boolean isDirectionInverted, String importStatusDetail) {
        return new CnecCreationContext(createdCnecId, isDirectionInverted, ImportStatus.IMPORTED, importStatusDetail);
    }

    public String getCreatedCnecId() {
        return createdCnecId;
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

    public boolean isDirectionInvertedInNetwork() {
        return isDirectionInverted;
    }
}

