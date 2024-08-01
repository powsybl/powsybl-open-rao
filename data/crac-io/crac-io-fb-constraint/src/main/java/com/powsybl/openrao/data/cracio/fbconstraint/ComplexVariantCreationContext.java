/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.RemedialActionCreationContext;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class ComplexVariantCreationContext implements RemedialActionCreationContext {

    private final String complexVariantId;
    private final String createdRaId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;

    @Override
    public String getNativeObjectId() {
        return complexVariantId;
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
    public String getCreatedObjectId() {
        return createdRaId;
    }

    ComplexVariantCreationContext(String complexVariantId, ImportStatus importStatus, String createdRaId, String importStatusDetail) {
        this.complexVariantId = complexVariantId;
        this.importStatus = importStatus;
        this.createdRaId = createdRaId;
        this.importStatusDetail = importStatusDetail;
    }
}
