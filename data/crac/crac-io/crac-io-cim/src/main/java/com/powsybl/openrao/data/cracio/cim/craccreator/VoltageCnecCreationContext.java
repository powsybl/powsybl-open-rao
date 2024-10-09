/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
// TODO : make it implement ElementaryCreationContext if we can extend ElementaryCreationContext?
public final class VoltageCnecCreationContext {
    private final String nativeNetworkElementId;
    private final String instantId;
    private final String nativeContingencyName;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final String createdCnecId;

    private VoltageCnecCreationContext(String nativeNetworkElementId, String instantId, String nativeContingencyName, ImportStatus importStatus, String importStatusDetail, String createdCnecId) {
        this.nativeNetworkElementId = nativeNetworkElementId;
        this.instantId = instantId;
        this.nativeContingencyName = nativeContingencyName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.createdCnecId = createdCnecId;
    }

    static VoltageCnecCreationContext imported(String nativeNetworkElementId, String instantId, String nativeContingencyName, String createdCnecId) {
        return new VoltageCnecCreationContext(nativeNetworkElementId, instantId, nativeContingencyName, ImportStatus.IMPORTED, null, createdCnecId);
    }

    static VoltageCnecCreationContext notImported(String nativeNetworkElementId, String instantId, String nativeContingencyName, ImportStatus importStatus, String importStatusDetail) {
        return new VoltageCnecCreationContext(nativeNetworkElementId, instantId, nativeContingencyName, importStatus, importStatusDetail, null);
    }

    public String getNativeNetworkElementId() {
        return nativeNetworkElementId;
    }

    public String getInstantId() {
        return instantId;
    }

    public String getNativeContingencyName() {
        return nativeContingencyName;
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public String getImportStatusDetail() {
        return importStatusDetail;
    }

    public String getCreatedCnecId() {
        return createdCnecId;
    }

    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }
}
