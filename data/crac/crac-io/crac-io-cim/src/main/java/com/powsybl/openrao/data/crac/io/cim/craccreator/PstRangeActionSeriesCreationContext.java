/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.cim.craccreator;

import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;

import java.util.Set;

public final class PstRangeActionSeriesCreationContext extends RemedialActionSeriesCreationContext {
    private final String networkElementNativeMrid;
    private final String networkElementNativeName;

    private PstRangeActionSeriesCreationContext(String nativeId, ImportStatus importStatus, boolean isAltered, String importStatusDetail,
                                                String networkElementNativeMrid, String networkElementNativeName) {
        super(nativeId, Set.of(nativeId), importStatus, isAltered, false, importStatusDetail);
        this.networkElementNativeMrid = networkElementNativeMrid;
        this.networkElementNativeName = networkElementNativeName;
    }

    public static PstRangeActionSeriesCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new PstRangeActionSeriesCreationContext(nativeId, importStatus, false, importStatusDetail, null, null);
    }

    public static PstRangeActionSeriesCreationContext imported(String nativeId, boolean isAltered, String importStatusDetail, String networkElementNativeMrid, String networkElementNativeName) {
        return new PstRangeActionSeriesCreationContext(nativeId, ImportStatus.IMPORTED, isAltered, importStatusDetail, networkElementNativeMrid, networkElementNativeName);
    }

    public String getNetworkElementNativeMrid() {
        return networkElementNativeMrid;
    }

    public String getNetworkElementNativeName() {
        return networkElementNativeName;
    }
}
