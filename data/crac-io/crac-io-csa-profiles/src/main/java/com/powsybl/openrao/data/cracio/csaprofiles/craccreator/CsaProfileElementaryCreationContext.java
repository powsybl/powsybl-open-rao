package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;

public final class CsaProfileElementaryCreationContext extends StandardElementaryCreationContext {
    private CsaProfileElementaryCreationContext(String nativeId, String elementId, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.nativeObjectId = nativeId;
        this.createdObjectId = elementId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    public static CsaProfileElementaryCreationContext imported(String nativeId, String elementId, String importStatusDetail, boolean isAltered) {
        return new CsaProfileElementaryCreationContext(nativeId, elementId, ImportStatus.IMPORTED, importStatusDetail, isAltered);
    }

    public static CsaProfileElementaryCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new CsaProfileElementaryCreationContext(nativeId, null, importStatus, importStatusDetail, false);
    }
}
