package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

public final class CsaProfileElementaryCreationContext implements ElementaryCreationContext {
    private final String nativeId;
    private final String elementId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final boolean isAltered;

    private CsaProfileElementaryCreationContext(String nativeId, String elementId, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.nativeId = nativeId;
        this.elementId = elementId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    public static CsaProfileElementaryCreationContext imported(String nativeId, String elementId, String elementName, String importStatusDetail, boolean isAltered) {
        return new CsaProfileElementaryCreationContext(nativeId, elementId, ImportStatus.IMPORTED, importStatusDetail, isAltered);
    }

    public static CsaProfileElementaryCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new CsaProfileElementaryCreationContext(nativeId, null, importStatus, importStatusDetail, false);
    }

    @Override
    public String getNativeObjectId() {
        return this.nativeId;
    }

    @Override
    public String getCreatedObjectId() {
        return this.elementId;
    }

    @Override
    public boolean isImported() {
        return ImportStatus.IMPORTED.equals(this.importStatus);
    }

    @Override
    public boolean isAltered() {
        return this.isAltered;
    }

    @Override
    public ImportStatus getImportStatus() {
        return this.importStatus;
    }

    @Override
    public String getImportStatusDetail() {
        return this.importStatusDetail;
    }
}
