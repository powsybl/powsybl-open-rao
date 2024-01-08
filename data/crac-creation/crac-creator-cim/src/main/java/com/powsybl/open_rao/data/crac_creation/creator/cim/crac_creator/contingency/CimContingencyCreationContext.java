/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.contingency;

import com.powsybl.open_rao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimContingencyCreationContext implements ElementaryCreationContext {
    private String contingencyID;
    private String contingencyName;
    private ImportStatus importStatus;
    private String createdContingencyId;
    private String importStatusDetail;
    private boolean isAltered;

    private CimContingencyCreationContext(String contingencyID, String contingencyName, ImportStatus importStatus, String createdContingencyID, boolean isAltered, String importStatusDetail) {
        this.contingencyID = contingencyID;
        this.contingencyName = contingencyName;
        this.importStatus = importStatus;
        this.createdContingencyId = createdContingencyID;
        this.isAltered = isAltered;
        this.importStatusDetail = importStatusDetail;
    }

    static CimContingencyCreationContext notImported(String contingencyID, String contingencyName, ImportStatus importStatus, String importStatusDetail) {
        return new CimContingencyCreationContext(contingencyID, contingencyName, importStatus, null, false, importStatusDetail);
    }

    static CimContingencyCreationContext imported(String contingencyID, String contingencyName, String createdContingencyId, boolean isAltered, String alteringDetail) {
        return new CimContingencyCreationContext(contingencyID, contingencyName, ImportStatus.IMPORTED, createdContingencyId, isAltered, alteringDetail);
    }

    @Override
    public String getNativeId() {
        return contingencyID;
    }

    public String getNativeName() {
        return contingencyName;
    }

    @Override
    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    @Override
    public boolean isAltered() {
        return isAltered;
    }

    @Override
    public ImportStatus getImportStatus() {
        return importStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return importStatusDetail;
    }

    public String getCreatedContingencyId() {
        return createdContingencyId;
    }
}

