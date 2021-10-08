/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.TRemedialAction;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CseRemedialActionCreationContext implements RemedialActionCreationContext {

    private final String nativeId;
    private final String createdRAId;
    private final boolean isAltered;
    private final ImportStatus importStatus;
    private final String importStatusDetail;

    protected CseRemedialActionCreationContext(String nativeId, String createdRAId, ImportStatus importStatus, boolean isAltered, String importStatusDetail) {
        this.nativeId = nativeId;
        this.createdRAId = createdRAId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    protected CseRemedialActionCreationContext(TRemedialAction tRemedialAction, String createdRAId, ImportStatus importStatus, boolean isAltered, String importStatusDetail) {
        this(tRemedialAction.getName().getV(), createdRAId, importStatus, isAltered, importStatusDetail);
    }

    public static CseRemedialActionCreationContext imported(TRemedialAction tRemedialAction, String createdRAId, boolean isAltered, String alteringDetail) {
        return new CseRemedialActionCreationContext(tRemedialAction, createdRAId, ImportStatus.IMPORTED, isAltered, alteringDetail);
    }

    public static CseRemedialActionCreationContext notImported(TRemedialAction tRemedialAction, ImportStatus importStatus, String importStatusDetail) {
        return new CseRemedialActionCreationContext(tRemedialAction, null, importStatus, false, importStatusDetail);
    }

    public static CseRemedialActionCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new CseRemedialActionCreationContext(nativeId, null, importStatus, false, importStatusDetail);
    }

    @Override
    public String getNativeId() {
        return nativeId;
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

    @Override
    public String getCreatedRAId() {
        return createdRAId;
    }

}
