/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.remedialaction;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.InjectionRangeActionCreationContext;
import com.powsybl.openrao.data.cracio.cse.xsd.TRemedialAction;

import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class CseHvdcCreationContext extends StandardElementaryCreationContext implements InjectionRangeActionCreationContext {

    private final String nativeFromGeneratorId;
    private final String nativeToGeneratorId;
    private final String powsyblFromGeneratorId;
    private final String powsyblToGeneratorId;

    private CseHvdcCreationContext(TRemedialAction tRemedialAction,
                                   String createdRaId,
                                   ImportStatus importStatus,
                                   String importStatusDetail,
                                   String nativeFromGeneratorId,
                                   String powsyblFromGeneratorId,
                                   String nativeToGeneratorId,
                                   String powsyblToGeneratorId) {

        super(tRemedialAction.getName().getV(), null, createdRaId, importStatus, importStatusDetail, false);
        this.nativeFromGeneratorId = nativeFromGeneratorId;
        this.nativeToGeneratorId = nativeToGeneratorId;
        this.powsyblFromGeneratorId = powsyblFromGeneratorId;
        this.powsyblToGeneratorId = powsyblToGeneratorId;
    }

    public static CseHvdcCreationContext imported(TRemedialAction tRemedialAction,
                                                  String createdRAId,
                                                  String nativeFromGeneratorId,
                                                  String powsyblFromGeneratorId,
                                                  String nativeToGeneratorId,
                                                  String powsyblToGeneratorId) {

        return new CseHvdcCreationContext(tRemedialAction, createdRAId, ImportStatus.IMPORTED, null, nativeFromGeneratorId, powsyblFromGeneratorId, nativeToGeneratorId, powsyblToGeneratorId);
    }

    public static CseHvdcCreationContext notImported(TRemedialAction tRemedialAction,
                                                     ImportStatus importStatus,
                                                     String importStatusDetail,
                                                     String nativeFromGeneratorId,
                                                     String nativeToGeneratorId) {
        return new CseHvdcCreationContext(tRemedialAction, null, importStatus, importStatusDetail, nativeFromGeneratorId, null, nativeToGeneratorId, null);
    }

    @Override
    public Map<String, String> getNativeNetworkElementIds() {
        return Map.of(nativeFromGeneratorId, powsyblFromGeneratorId, nativeToGeneratorId, powsyblToGeneratorId);
    }
}
