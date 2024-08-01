/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
// TODO : make it implement ElementaryCreationContext if we can extend ElementaryCreationContext?
public final class MonitoredSeriesCreationContext {
    private final String monitoredSeriesId;
    private final String monitoredSeriesName;
    private final String registeredResourceId;
    private final String registeredResourceName;
    private ImportStatus importStatus;
    private String importStatusDetail;
    private final Set<MeasurementCreationContext> measurementCreationContexts;
    private final boolean isAltered;

    MonitoredSeriesCreationContext(
        String monitoredSeriesId,
        String monitoredSeriesName,
        String registeredResourceId,
        String registeredResourceName,
        ImportStatus importStatus,
        boolean isAltered,
        String importStatusDetail) {
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

    void addMeasurementCreationContext(MeasurementCreationContext measurementCreationContext) {
        addMeasurementCreationContexts(Set.of(measurementCreationContext));
    }

    void addMeasurementCreationContexts(Set<MeasurementCreationContext> measurementCreationContexts) {
        this.measurementCreationContexts.addAll(measurementCreationContexts);
        if (this.measurementCreationContexts.stream().anyMatch(MeasurementCreationContext::isImported)) {
            this.importStatus = ImportStatus.IMPORTED;
            if (!isAltered) {
                this.importStatusDetail = "";
            }
        } else {
            this.importStatus = ImportStatus.OTHER;
            this.importStatusDetail = "None of the measurements could be imported";
        }
    }

    public boolean isAltered() {
        return isAltered;
    }

    public Set<String> getCreatedCnecIds() {
        return measurementCreationContexts.stream().map(mcc ->
            mcc.getCnecCreationContexts().values().stream().map(CnecCreationContext::getCreatedCnecId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).flatMap(Set::stream).collect(Collectors.toSet());
    }
}

