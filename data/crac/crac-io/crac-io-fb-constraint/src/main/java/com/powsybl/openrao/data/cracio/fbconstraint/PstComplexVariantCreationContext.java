/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.PstRangeActionCreationContext;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public final class PstComplexVariantCreationContext extends StandardElementaryCreationContext implements PstRangeActionCreationContext {
    private final boolean isInverted;
    private final String nativeNetworkElementId;

    private PstComplexVariantCreationContext(String complexVariantId, String nativeNetworkElementId, String createdRaId, ImportStatus importStatus, boolean isInverted, String importStatusDetail) {
        super(complexVariantId, null, createdRaId, importStatus, importStatusDetail, false);
        this.isInverted = isInverted;
        this.nativeNetworkElementId = nativeNetworkElementId;
    }

    public static PstComplexVariantCreationContext imported(String complexVariantId, String nativeNetworkElementId, String createdRaId, boolean isInverted, String inversionMessage) {
        return new PstComplexVariantCreationContext(complexVariantId, nativeNetworkElementId, createdRaId, ImportStatus.IMPORTED, isInverted, inversionMessage);
    }

    @Override
    public boolean isInverted() {
        return isInverted;
    }

    @Override
    public String getNativeNetworkElementId() {
        return nativeNetworkElementId;
    }
}
