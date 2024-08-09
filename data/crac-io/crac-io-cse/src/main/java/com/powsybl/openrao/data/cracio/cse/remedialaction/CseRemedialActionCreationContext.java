/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.remedialaction;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.cse.xsd.TRemedialAction;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CseRemedialActionCreationContext extends StandardElementaryCreationContext {

    protected CseRemedialActionCreationContext(String nativeId, String createdRAId, ImportStatus importStatus, boolean isAltered, String importStatusDetail) {
        this.nativeObjectId = nativeId;
        this.createdObjectId = createdRAId;
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
}
