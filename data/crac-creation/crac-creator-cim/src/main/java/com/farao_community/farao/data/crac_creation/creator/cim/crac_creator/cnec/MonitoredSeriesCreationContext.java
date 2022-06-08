/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MonitoredSeriesCreationContext {
    private final String monitoredSeriesId;
    private final String monitoredSeriesName;
    private final String registeredResourceId;
    private final String registeredResourceName;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final Set<MeasurementCreationContext> measurementCreationContexts;
    private final boolean isAltered;

    private MonitoredSeriesCreationContext(String monitoredSeriesId, String monitoredSeriesName, String registeredResourceId, String registeredResourceName,
                                           ImportStatus importStatus, boolean isAltered, String importStatusDetail) {
        this.monitoredSeriesId = monitoredSeriesId;
        this.monitoredSeriesName = monitoredSeriesName;
        this.registeredResourceId = registeredResourceId;
        this.registeredResourceName = registeredResourceName;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        measurementCreationContexts = new LinkedHashSet<>();
        this.isAltered = isAltered;
    }

    static MonitoredSeriesCreationContext notImported(String monitoredSeriesId, String monitoredSeriesName, String registeredResourceId, String registeredResourceName,
                                                      ImportStatus importStatus, String importStatusDetail) {
        return new MonitoredSeriesCreationContext(monitoredSeriesId, monitoredSeriesName, registeredResourceId, registeredResourceName, importStatus, false, importStatusDetail);
    }

    static MonitoredSeriesCreationContext imported(String monitoredSeriesId, String monitoredSeriesName, String registeredResourceId, String registeredResourceName,
                                                   boolean isAltered, String importStatusDetail) {
        return new MonitoredSeriesCreationContext(monitoredSeriesId, monitoredSeriesName, registeredResourceId, registeredResourceName, ImportStatus.IMPORTED, isAltered, importStatusDetail);
    }

    public String getNativeId() {
        return monitoredSeriesId;
    }

    public String getNativeName() {
        return monitoredSeriesName;
    }

    public String getNativeResourceId() {
        return registeredResourceId;
    }

    public String getNativeResourceName() {
        return registeredResourceName;
    }

    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public String getImportStatusDetail() {
        return importStatusDetail;
    }

    public Set<MeasurementCreationContext> getMeasurementCreationContexts() {
        return measurementCreationContexts;
    }

    public void addMeasurementCreationContext(MeasurementCreationContext measurementCreationContext) {
        measurementCreationContexts.add(measurementCreationContext);
    }

    public boolean isAltered() {
        return isAltered;
    }
}

