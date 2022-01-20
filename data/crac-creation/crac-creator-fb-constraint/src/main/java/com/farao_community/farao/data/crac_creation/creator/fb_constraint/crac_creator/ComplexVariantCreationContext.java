/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class ComplexVariantCreationContext implements RemedialActionCreationContext {

    private final String complexVariantId;
    private final String createdRaId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;

    @Override
    public String getNativeId() {
        return complexVariantId;
    }

    @Override
    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
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
    public String getCreatedRAId() {
        return createdRaId;
    }

    ComplexVariantCreationContext(String complexVariantId, ImportStatus importStatus, String createdRaId, String importStatusDetail) {
        this.complexVariantId = complexVariantId;
        this.importStatus = importStatus;
        this.createdRaId = createdRaId;
        this.importStatusDetail = importStatusDetail;
    }
}
