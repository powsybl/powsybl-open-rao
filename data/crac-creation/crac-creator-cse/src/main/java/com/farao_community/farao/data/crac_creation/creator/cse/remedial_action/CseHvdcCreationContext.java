/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.HvdcRangeActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.xsd.TRemedialAction;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CseHvdcCreationContext extends CseRemedialActionCreationContext implements HvdcRangeActionCreationContext {

    private final String nativeNetworkElementId;
    private final boolean isInverted;

    private CseHvdcCreationContext(TRemedialAction tRemedialAction, String nativeNetworkElementId, String createdRaId, ImportStatus importStatus, boolean isInverted, String importStatusDetail) {
        super(tRemedialAction, createdRaId, importStatus, false, importStatusDetail);
        this.nativeNetworkElementId = nativeNetworkElementId;
        this.isInverted = isInverted;
    }

    public static CseHvdcCreationContext imported(TRemedialAction tRemedialAction, String nativeNetworkElementId, String createdRAId, boolean isInverted) {
        return new CseHvdcCreationContext(tRemedialAction, nativeNetworkElementId, createdRAId, ImportStatus.IMPORTED, isInverted, null);
    }

    public static CseHvdcCreationContext notImported(TRemedialAction tRemedialAction, String nativeNetworkElementId, ImportStatus importStatus, String importStatusDetail) {
        return new CseHvdcCreationContext(tRemedialAction, nativeNetworkElementId, null, importStatus, false, importStatusDetail);
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
