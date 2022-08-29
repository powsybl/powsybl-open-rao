/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class VoltageCnecCreationContext {
    private final String nativeNetworkElementId;
    private final Instant instant;
    private final String nativeContingencyName;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final String createdCnecId;

    private VoltageCnecCreationContext(String nativeNetworkElementId, Instant instant, String nativeContingencyName, ImportStatus importStatus, String importStatusDetail, String createdCnecId) {
        this.nativeNetworkElementId = nativeNetworkElementId;
        this.instant = instant;
        this.nativeContingencyName = nativeContingencyName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.createdCnecId = createdCnecId;
    }

    public static VoltageCnecCreationContext imported(String nativeNetworkElementId, Instant instant, String nativeContingencyName, String createdCnecId) {
        return new VoltageCnecCreationContext(nativeNetworkElementId, instant, nativeContingencyName, ImportStatus.IMPORTED, null, createdCnecId);
    }

    public static VoltageCnecCreationContext notImported(String nativeNetworkElementId, Instant instant, String nativeContingencyName, ImportStatus importStatus, String importStatusDetail) {
        return new VoltageCnecCreationContext(nativeNetworkElementId, instant, nativeContingencyName, importStatus, importStatusDetail, null);
    }

    public String getNativeNetworkElementId() {
        return nativeNetworkElementId;
    }

    public Instant getInstant() {
        return instant;
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
