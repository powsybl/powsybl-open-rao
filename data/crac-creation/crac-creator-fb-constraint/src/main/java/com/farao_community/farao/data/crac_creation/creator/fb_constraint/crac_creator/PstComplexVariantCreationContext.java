/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.PstRangeActionCreationContext;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public final class PstComplexVariantCreationContext extends ComplexVariantCreationContext implements PstRangeActionCreationContext {
    private final boolean isInverted;
    private final String nativeNetworkElementId;

    private PstComplexVariantCreationContext(String complexVariantId, String nativeNetworkElementId, String createdRaId, ImportStatus importStatus, boolean isInverted, String importStatusDetail) {
        super(complexVariantId, importStatus, createdRaId, importStatusDetail);
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
