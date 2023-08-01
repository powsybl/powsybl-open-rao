/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileCnecCreationContext implements ElementaryCreationContext {
    private String nativeId;
    private String flowCnecId;
    private String flowCnecName;
    private ImportStatus importStatus;
    private String importStatusDetail;
    private boolean isAltered;

    private CsaProfileCnecCreationContext(String nativeId, String flowCnecId, String flowCnecName, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.nativeId = nativeId;
        this.flowCnecId = flowCnecId;
        this.flowCnecName = flowCnecName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    public static CsaProfileCnecCreationContext imported(String nativeId, String flowCnecId, String flowCnecName, String importStatusDetail, boolean isAltered) {
        return new CsaProfileCnecCreationContext(nativeId, flowCnecId, flowCnecName, ImportStatus.IMPORTED, importStatusDetail, isAltered);
    }

    public static CsaProfileCnecCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new CsaProfileCnecCreationContext(nativeId, null, null, importStatus, importStatusDetail, false);
    }

    @Override
    public String getNativeId() {
        return nativeId;
    }

    public String getFlowCnecId() {
        return flowCnecId;
    }

    public String getFlowCnecName() {
        return flowCnecName;
    }

    @Override
    public boolean isImported() {
        return ImportStatus.IMPORTED.equals(this.importStatus);
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
}
