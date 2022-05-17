/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreationContext;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCracCreationContext implements CracCreationContext {
    private Crac crac;
    private boolean isCreationSuccessful;
    private Set<CimContingencyCreationContext> contingencyCreationContexts;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts;
    private CracCreationReport creationReport;
    private OffsetDateTime timeStamp;

    public CimCracCreationContext(Crac crac, OffsetDateTime timeStamp) {
        this.crac = crac;
        creationReport = new CracCreationReport();
        this.timeStamp = timeStamp;
    }

    protected CimCracCreationContext(CimCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.contingencyCreationContexts = new HashSet<>(toCopy.contingencyCreationContexts);
        this.monitoredSeriesCreationContexts = toCopy.monitoredSeriesCreationContexts;
        this.remedialActionSeriesCreationContexts = new HashSet<>(toCopy.remedialActionSeriesCreationContexts);
        this.creationReport = toCopy.creationReport;
        this.timeStamp = toCopy.timeStamp;
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
    public void buildCreationReport() {
        addToReport(contingencyCreationContexts, "Contingency_Series");
        addToReport(monitoredSeriesCreationContexts);
        addToReport(remedialActionSeriesCreationContexts, "RemedialAction_Series");
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered).forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported()).forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }

    private void addToReport(Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts) {
        for (String monitoredSeriesId : monitoredSeriesCreationContexts.keySet()) {
            MonitoredSeriesCreationContext monitoredSeriesCreationContext = monitoredSeriesCreationContexts.get(monitoredSeriesId);
            if (!monitoredSeriesCreationContext.isImported()) {
                creationReport.removed(String.format("Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesCreationContext.getNativeId(),
                    monitoredSeriesCreationContext.getImportStatus(), monitoredSeriesCreationContext.getImportStatusDetail()));
            } else {
                if (monitoredSeriesCreationContext.isAltered()) {
                    creationReport.altered(String.format("Monitored_Series \"%s\" was altered : %s.", monitoredSeriesCreationContext.getNativeId(),
                        monitoredSeriesCreationContext.getImportStatusDetail()));
                }
                for (MeasurementCreationContext measurementCreationContext : monitoredSeriesCreationContext.getMeasurementCreationContexts()) {
                    if (!measurementCreationContext.isImported()) {
                        creationReport.removed(String.format("A Measurement in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesCreationContext.getNativeId(),
                            measurementCreationContext.getImportStatus(), measurementCreationContext.getImportStatusDetail()));
                    } else {
                        for (CnecCreationContext cnecCreationContext : measurementCreationContext.getCnecCreationContexts().values()) {
                            if (!cnecCreationContext.isImported()) {
                                creationReport.removed(String.format("A Cnec in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesCreationContext.getNativeId(),
                                    cnecCreationContext.getImportStatus(), cnecCreationContext.getImportStatusDetail()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }

    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setContingencyCreationContexts(Set<CimContingencyCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = new HashSet<>(contingencyCreationContexts);
    }

    public Set<CimContingencyCreationContext> getContingencyCreationContexts() {
        return new HashSet<>(contingencyCreationContexts);
    }

    public MonitoredSeriesCreationContext getMonitoredSeriesCreationContext(String seriesId) {
        return monitoredSeriesCreationContexts.get(seriesId);
    }

    public Map<String, MonitoredSeriesCreationContext> getMonitoredSeriesCreationContexts() {
        return monitoredSeriesCreationContexts;
    }

    public void setMonitoredSeriesCreationContexts(Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts) {
        this.monitoredSeriesCreationContexts = monitoredSeriesCreationContexts;
    }

    public void setRemedialActionSeriesCreationContexts(Set<RemedialActionSeriesCreationContext> remedialActionCreationContexts) {
        this.remedialActionSeriesCreationContexts = new HashSet<>(remedialActionCreationContexts);
    }

    public Set<RemedialActionSeriesCreationContext> getRemedialActionSeriesCreationContexts() {
        return remedialActionSeriesCreationContexts;
    }

    public RemedialActionSeriesCreationContext getRemedialActionSeriesCreationContexts(String seriesId) {
        return remedialActionSeriesCreationContexts.stream().filter(creationContext -> creationContext.getNativeId().equals(seriesId)).findAny().orElseThrow();
    }

    public CimContingencyCreationContext getContingencyCreationContext(String contingencyName) {
        return contingencyCreationContexts.stream().filter(contingencyCreationContext -> contingencyCreationContext.getNativeId().equals(contingencyName)).findAny().orElseThrow();
    }

    CimCracCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    CimCracCreationContext creationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
        return this;
    }
}
