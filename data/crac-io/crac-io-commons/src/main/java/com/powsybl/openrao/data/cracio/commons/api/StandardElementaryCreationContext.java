/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class StandardElementaryCreationContext implements ElementaryCreationContext {
    protected String nativeObjectId;
    protected String nativeObjectName;
    protected String createdObjectId;
    protected ImportStatus importStatus;
    protected String importStatusDetail;
    protected boolean isAltered;

    public StandardElementaryCreationContext(String nativeObjectId, String nativeObjectName, String createdObjectId, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.nativeObjectId = nativeObjectId;
        this.nativeObjectName = nativeObjectName;
        this.createdObjectId = createdObjectId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    @Override
    public String getNativeObjectId() {
        return nativeObjectId;
    }

    @Override
    public Optional<String> getNativeObjectName() {
        return Optional.ofNullable(nativeObjectName);
    }

    @Override
    public String getCreatedObjectId() {
        return createdObjectId;
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
    public boolean isAltered() {
        return isAltered;
    }
}
