/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RemedialActionSeriesCreationContext extends StandardElementaryCreationContext {
    private final Set<String> createdIds;
    private final boolean isInverted;

    protected RemedialActionSeriesCreationContext(String nativeId, Set<String> createdIds, ImportStatus importStatus, boolean isAltered, boolean isInverted, String importStatusDetail) {
        super(nativeId, null, null, importStatus, importStatusDetail, isAltered);
        this.createdIds = createdIds;
        this.isInverted = isInverted;
    }

    static RemedialActionSeriesCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, Set.of(nativeId), importStatus, false, false, importStatusDetail);
    }

    static RemedialActionSeriesCreationContext imported(String nativeId, boolean isAltered, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, Set.of(nativeId), ImportStatus.IMPORTED, isAltered, false, importStatusDetail);
    }

    static RemedialActionSeriesCreationContext importedHvdcRa(String nativeId, Set<String> createdIds, boolean isAltered, boolean isInverted, String importStatusDetail) {
        return new RemedialActionSeriesCreationContext(nativeId, createdIds, ImportStatus.IMPORTED, isAltered, isInverted, importStatusDetail);
    }

    public boolean isInverted() {
        return isInverted;
    }

    @Override
    public String getCreatedObjectId() {
        throw new NotImplementedException("Several objects may have been created. Please use getCreatedObjectsIds() instead.");
    }

    @Override
    public Set<String> getCreatedObjectsIds() {
        return createdIds;
    }
}

