/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class CsaProfileRemedialActionCreationContext implements ElementaryCreationContext {
    private final String nativeId;
    private final String remedialActionId;
    private final String remedialActionName;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final boolean isAltered;

    private CsaProfileRemedialActionCreationContext(String nativeId, String remedialActionId, String remedialActionName, ImportStatus importStatus, String importStatusDetail, boolean isAltered) {
        this.nativeId = nativeId;
        this.remedialActionId = remedialActionId;
        this.remedialActionName = remedialActionName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        this.isAltered = isAltered;
    }

    public static CsaProfileRemedialActionCreationContext imported(String nativeId, String remedialActionId, String remedialActionName, String importStatusDetail, boolean isAltered) {
        return new CsaProfileRemedialActionCreationContext(nativeId, remedialActionId, remedialActionName, ImportStatus.IMPORTED, importStatusDetail, isAltered);
    }

    public static CsaProfileRemedialActionCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new CsaProfileRemedialActionCreationContext(nativeId, null, null, importStatus, importStatusDetail, false);
    }

    @Override
    public String getNativeId() {
        return this.nativeId;
    }

    public String getRemedialActionId() {
        return this.remedialActionId;
    }

    public String getNativeName() {
        return this.remedialActionName;
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
