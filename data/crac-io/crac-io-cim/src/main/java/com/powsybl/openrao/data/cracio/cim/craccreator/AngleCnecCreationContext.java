/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AngleCnecCreationContext extends StandardElementaryCreationContext {
    private final String contingencyID;
    private final String serieId;

    private AngleCnecCreationContext(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus angleCnecImportStatus, String angleCnecImportStatusDetail) {
        super(createdAngleCnecId, null, createdAngleCnecId, angleCnecImportStatus, angleCnecImportStatusDetail, false);
        this.contingencyID = contingencyID;
        this.serieId = serieId;
    }

    static AngleCnecCreationContext notImported(String createdAngleCnecId, String contingencyID, String serieId, ImportStatus angleCnecImportStatus, String angleCnecImportStatusDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, angleCnecImportStatus, angleCnecImportStatusDetail);
    }

    static AngleCnecCreationContext imported(String createdAngleCnecId, String contingencyID, String serieId, String alteringDetail) {
        return new AngleCnecCreationContext(createdAngleCnecId, contingencyID, serieId, ImportStatus.IMPORTED, alteringDetail);
    }

    public String getContingencyId() {
        return contingencyID;
    }

    public String getSerieId() {
        return serieId;
    }
}

