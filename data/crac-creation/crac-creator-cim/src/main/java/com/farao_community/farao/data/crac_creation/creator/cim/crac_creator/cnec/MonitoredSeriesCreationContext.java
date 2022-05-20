/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MonitoredSeriesCreationContext {
    private final String monitoredSeriesId;
    private String inZoneId;
    private String outZoneId;
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final Set<MeasurementCreationContext> measurementCreationContexts;
    private final boolean isAltered;

    private MonitoredSeriesCreationContext(
        String monitoredSeriesId,
        String inZoneId,
        String outZoneId,
        ImportStatus importStatus,
        boolean isAltered,
        String importStatusDetail) {
        this.monitoredSeriesId = monitoredSeriesId;
        this.inZoneId = inZoneId;
        this.outZoneId = outZoneId;
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        measurementCreationContexts = new LinkedHashSet<>();
        this.isAltered = isAltered;
    }

    static MonitoredSeriesCreationContext notImported(String monitoredSeriesId, ImportStatus importStatus, String importStatusDetail) {
        return new MonitoredSeriesCreationContext(monitoredSeriesId, null, null, importStatus, false, importStatusDetail);
    }

    static MonitoredSeriesCreationContext imported(String monitoredSeriesId, String inZoneId, String outZoneId, boolean isAltered, String importStatusDetail) {
        return new MonitoredSeriesCreationContext(monitoredSeriesId, inZoneId, outZoneId, ImportStatus.IMPORTED, isAltered, importStatusDetail);
    }

    public String getNativeId() {
        return monitoredSeriesId;
    }

    public String getInZoneId() {
        return inZoneId;
    }

    public String getOutZoneId() {
        return outZoneId;
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

    public Set<String> getCreatedCnecIds() {
        return measurementCreationContexts.stream().map(mcc ->
            mcc.getCnecCreationContexts().values().stream().map(CnecCreationContext::getCreatedCnecId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).flatMap(Set::stream).collect(Collectors.toSet());
    }
}

