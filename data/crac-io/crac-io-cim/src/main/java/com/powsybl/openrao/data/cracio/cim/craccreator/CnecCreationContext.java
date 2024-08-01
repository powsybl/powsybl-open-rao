/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CnecCreationContext {
    private final String createdCnecId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;

    private CnecCreationContext(String createdCnecId, ImportStatus importStatus, String importStatusDetail) {
        this.createdCnecId = createdCnecId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
    }

    static CnecCreationContext notImported(ImportStatus importStatus, String importStatusDetail) {
        return new CnecCreationContext(null, importStatus, importStatusDetail);
    }

    static CnecCreationContext imported(String createdCnecId) {
        return new CnecCreationContext(createdCnecId, ImportStatus.IMPORTED, null);
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
}

