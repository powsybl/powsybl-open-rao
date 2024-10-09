/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

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
        this.nativeObjectName = nativeObjectName == null ? nativeObjectId : nativeObjectName;
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
    public String getNativeObjectName() {
        return nativeObjectName;
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

    public static StandardElementaryCreationContext notImported(String nativeObjectId, String nativeObjectName, ImportStatus importStatus, String importStatusDetail) {
        return new StandardElementaryCreationContext(nativeObjectId, nativeObjectName, null, importStatus, importStatusDetail, false);
    }

    public static StandardElementaryCreationContext imported(String nativeObjectId, String nativeObjectName, String createdObjectId, boolean isAltered, String alteringDetail) {
        return new StandardElementaryCreationContext(nativeObjectId, nativeObjectName, createdObjectId, ImportStatus.IMPORTED, alteringDetail, isAltered);
    }
}
