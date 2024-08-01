/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.outage;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseOutageCreationContext implements ElementaryCreationContext {
    private final String outageID;
    private final ImportStatus importStatus;
    private final String importStatusDetail;

    public CseOutageCreationContext(String outageId, ImportStatus importStatus, String importStatusDetail) {
        this.outageID = outageId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
    }

    public String getName() {
        return getNativeObjectId();
    }

    @Override
    public String getNativeObjectId() {
        return outageID;
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
    public String getCreatedObjectId() {
        return isImported() ? outageID : null;
    }

    static CseOutageCreationContext notImported(String outageID, ImportStatus importStatus, String importStatusDetail) {
        return new CseOutageCreationContext(outageID, importStatus, importStatusDetail);
    }

    static CseOutageCreationContext imported(String outageID) {
        return new CseOutageCreationContext(outageID, ImportStatus.IMPORTED, null);
    }
}
