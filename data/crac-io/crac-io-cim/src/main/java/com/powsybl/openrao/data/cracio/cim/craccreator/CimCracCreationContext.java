/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.CracCreationReport;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCracCreationContext implements CracCreationContext {
    private Crac crac;
    private boolean isCreationSuccessful;
    private Set<ElementaryCreationContext> contingencyCreationContexts;
    private final Set<AngleCnecCreationContext> angleCnecCreationContexts;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private final Set<VoltageCnecCreationContext> voltageCnecCreationContexts;
    private Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts;
    private final CracCreationReport creationReport;
    private final OffsetDateTime timeStamp;
    private final String networkName;

    CimCracCreationContext(Crac crac, OffsetDateTime timeStamp, String networkName) {
        this.crac = crac;
        creationReport = new CracCreationReport();
        this.timeStamp = timeStamp;
        this.angleCnecCreationContexts = new HashSet<>();
        this.voltageCnecCreationContexts = new HashSet<>();
        this.networkName = networkName;
    }

    protected CimCracCreationContext(CimCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.contingencyCreationContexts = new HashSet<>(toCopy.contingencyCreationContexts);
        this.monitoredSeriesCreationContexts = toCopy.monitoredSeriesCreationContexts;
        this.angleCnecCreationContexts = new HashSet<>(toCopy.angleCnecCreationContexts);
        this.voltageCnecCreationContexts = new HashSet<>(toCopy.voltageCnecCreationContexts);
        this.remedialActionSeriesCreationContexts = new HashSet<>(toCopy.remedialActionSeriesCreationContexts);
        this.creationReport = toCopy.creationReport;
        this.timeStamp = toCopy.timeStamp;
        this.networkName = toCopy.networkName;
    }

    @Override
    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    // Only contains contingency creation context report for the moment
    void buildCreationReport() {
        addToReport(contingencyCreationContexts, "Contingency_Series");
        addToReport(angleCnecCreationContexts, "AdditionalConstraint_Series");
        addToReport(monitoredSeriesCreationContexts);
        addToReport(remedialActionSeriesCreationContexts, "RemedialAction_Series");
        addToReport(voltageCnecCreationContexts);
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered).forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported()).forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }

    private void addToReport(Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts) {
        for (MonitoredSeriesCreationContext monitoredSeriesCreationContext : monitoredSeriesCreationContexts.values()) {
            if (!monitoredSeriesCreationContext.isImported()) {
                creationReport.removed(String.format("Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesCreationContext.getNativeId(),
                    monitoredSeriesCreationContext.getImportStatus(), monitoredSeriesCreationContext.getImportStatusDetail()));
            } else {
                if (monitoredSeriesCreationContext.isAltered()) {
                    creationReport.altered(String.format("Monitored_Series \"%s\" was altered : %s.", monitoredSeriesCreationContext.getNativeId(),
                        monitoredSeriesCreationContext.getImportStatusDetail()));
                }
                addToReport(monitoredSeriesCreationContext.getMeasurementCreationContexts(), monitoredSeriesCreationContext.getNativeId());
            }
        }
    }

    private void addToReport(Set<MeasurementCreationContext> measurementCreationContexts, String monitoredSeriesNativeId) {
        for (MeasurementCreationContext measurementCreationContext : measurementCreationContexts) {
            if (!measurementCreationContext.isImported()) {
                creationReport.removed(String.format("A Measurement in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesNativeId,
                    measurementCreationContext.getImportStatus(), measurementCreationContext.getImportStatusDetail()));
            } else {
                for (CnecCreationContext cnecCreationContext : measurementCreationContext.getCnecCreationContexts().values()) {
                    if (!cnecCreationContext.isImported()) {
                        creationReport.removed(String.format("A Cnec in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesNativeId,
                            cnecCreationContext.getImportStatus(), cnecCreationContext.getImportStatusDetail()));
                    }
                }
            }
        }
    }

    private void addToReport(Set<VoltageCnecCreationContext> voltageCnecCreationContexts) {
        voltageCnecCreationContexts.stream().filter(context -> !context.isImported()).forEach(context -> {
                String neId = context.getNativeNetworkElementId() != null ? context.getNativeNetworkElementId() : "all";
                String instant = context.getInstantId() != null ? context.getInstantId().toLowerCase() : "all";
                String coName = context.getNativeContingencyName() != null ? context.getNativeContingencyName() : "all";
                creationReport.removed(String.format("VoltageCnec with network element \"%s\", instant \"%s\" and contingency \"%s\" was not imported: %s. %s.", neId, instant, coName, context.getImportStatus(), context.getImportStatusDetail()));
            }
        );
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    void addAngleCnecCreationContext(AngleCnecCreationContext angleCnecCreationContext) {
        this.angleCnecCreationContexts.add(angleCnecCreationContext);
    }

    public Set<AngleCnecCreationContext> getAngleCnecCreationContexts() {
        return new HashSet<>(angleCnecCreationContexts);
    }

    public AngleCnecCreationContext getAngleCnecCreationContext(String seriesId) {
        return angleCnecCreationContexts.stream().filter(creationContext -> creationContext.getNativeObjectId().equals(seriesId)).findAny().orElse(null);
    }

    void addVoltageCnecCreationContext(VoltageCnecCreationContext voltageCnecCreationContext) {
        this.voltageCnecCreationContexts.add(voltageCnecCreationContext);
    }

    public Set<VoltageCnecCreationContext> getVoltageCnecCreationContexts() {
        return new HashSet<>(voltageCnecCreationContexts);
    }

    public VoltageCnecCreationContext getVoltageCnecCreationContext(String nativeNetworkElementId, String instantId, String nativeContingencyName) {
        return voltageCnecCreationContexts.stream().filter(creationContext ->
            nativeNetworkElementId.equals(creationContext.getNativeNetworkElementId())
                && instantId.equals(creationContext.getInstantId())
                && (nativeContingencyName == null && creationContext.getNativeContingencyName() == null || nativeContingencyName != null && nativeContingencyName.equals(creationContext.getNativeContingencyName()))
        ).findAny().orElse(null);
    }

    public Set<VoltageCnecCreationContext> getVoltageCnecCreationContextsForNetworkElement(String nativeNetworkElementId) {
        return voltageCnecCreationContexts.stream().filter(creationContext -> nativeNetworkElementId.equals(creationContext.getNativeNetworkElementId())).collect(Collectors.toSet());
    }

    public Set<VoltageCnecCreationContext> getVoltageCnecCreationContextsForContingency(String nativeContingencyName) {
        return voltageCnecCreationContexts.stream().filter(creationContext -> nativeContingencyName.equals(creationContext.getNativeContingencyName())).collect(Collectors.toSet());
    }

    void setContingencyCreationContexts(Set<ElementaryCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = new HashSet<>(contingencyCreationContexts);
    }

    public Set<ElementaryCreationContext> getContingencyCreationContexts() {
        return new HashSet<>(contingencyCreationContexts);
    }

    public MonitoredSeriesCreationContext getMonitoredSeriesCreationContext(String seriesId) {
        return monitoredSeriesCreationContexts.get(seriesId);
    }

    public Map<String, MonitoredSeriesCreationContext> getMonitoredSeriesCreationContexts() {
        return new HashMap<>(monitoredSeriesCreationContexts);
    }

    void setMonitoredSeriesCreationContexts(Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts) {
        this.monitoredSeriesCreationContexts = monitoredSeriesCreationContexts;
    }

    void setRemedialActionSeriesCreationContexts(Set<RemedialActionSeriesCreationContext> remedialActionCreationContexts) {
        this.remedialActionSeriesCreationContexts = new HashSet<>(remedialActionCreationContexts);
    }

    public Set<RemedialActionSeriesCreationContext> getRemedialActionSeriesCreationContexts() {
        return new HashSet<>(remedialActionSeriesCreationContexts);
    }

    public RemedialActionSeriesCreationContext getRemedialActionSeriesCreationContext(String seriesId) {
        return remedialActionSeriesCreationContexts.stream().filter(creationContext -> creationContext.getNativeObjectId().equals(seriesId)).findAny().orElse(null);
    }

    public ElementaryCreationContext getContingencyCreationContextById(String contingencyId) {
        return contingencyCreationContexts.stream().filter(contingencyCreationContext -> contingencyCreationContext.getNativeObjectId().equals(contingencyId)).findAny().orElse(null);
    }

    public ElementaryCreationContext getContingencyCreationContextByName(String contingencyName) {
        return contingencyCreationContexts.stream().filter(contingencyCreationContext -> contingencyCreationContext.getNativeObjectName().equals(contingencyName)).findAny().orElse(null);
    }

    void setCreationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
    }

    void setCreationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
    }
}
