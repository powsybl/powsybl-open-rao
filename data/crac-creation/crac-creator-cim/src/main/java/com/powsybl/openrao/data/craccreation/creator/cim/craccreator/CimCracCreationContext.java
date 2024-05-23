/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim.craccreator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationReport;
import com.powsybl.openrao.data.craccreation.creator.api.ElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.cnec.*;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.contingency.CimContingencyCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.remedialaction.RemedialActionSeriesCreationContext;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCracCreationContext implements CracCreationContext {
    private Crac crac;
    private boolean isCreationSuccessful;
    private Set<CimContingencyCreationContext> contingencyCreationContexts;
    private final Set<AngleCnecCreationContext> angleCnecCreationContexts;
    private Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts;
    private final Set<VoltageCnecCreationContext> voltageCnecCreationContexts;
    private Set<RemedialActionSeriesCreationContext> remedialActionSeriesCreationContexts;
    private final OffsetDateTime timeStamp;
    private final String networkName;

    CimCracCreationContext(Crac crac, OffsetDateTime timeStamp, String networkName) {
        this.crac = crac;
        this.timeStamp = timeStamp;
        this.angleCnecCreationContexts = new LinkedHashSet<>();
        this.voltageCnecCreationContexts = new LinkedHashSet<>();
        this.networkName = networkName;
    }

    protected CimCracCreationContext(CimCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.contingencyCreationContexts = new LinkedHashSet<>(toCopy.contingencyCreationContexts);
        this.monitoredSeriesCreationContexts = toCopy.monitoredSeriesCreationContexts;
        this.angleCnecCreationContexts = new LinkedHashSet<>(toCopy.angleCnecCreationContexts);
        this.voltageCnecCreationContexts = new LinkedHashSet<>(toCopy.voltageCnecCreationContexts);
        this.remedialActionSeriesCreationContexts = new LinkedHashSet<>(toCopy.remedialActionSeriesCreationContexts);
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
    public void buildCreationReport(ReportNode reportNode) {
        addToReport(contingencyCreationContexts, "Contingency_Series", reportNode);
        addToReport(angleCnecCreationContexts, "AdditionalConstraint_Series", reportNode);
        addToReport(monitoredSeriesCreationContexts, reportNode);
        addToReport(remedialActionSeriesCreationContexts, "RemedialAction_Series", reportNode);
        addToReport(voltageCnecCreationContexts, reportNode);
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier, ReportNode reportNode) {
        contexts.stream().filter(ElementaryCreationContext::isAltered).forEach(context ->
            CracCreationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeId(), context.getImportStatusDetail()), reportNode)
        );
        contexts.stream().filter(context -> !context.isImported()).forEach(context ->
            CracCreationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeId(), context.getImportStatus(), context.getImportStatusDetail()), reportNode)
        );
    }

    private void addToReport(Map<String, MonitoredSeriesCreationContext> monitoredSeriesCreationContexts, ReportNode reportNode) {
        for (MonitoredSeriesCreationContext monitoredSeriesCreationContext : monitoredSeriesCreationContexts.values()) {
            if (!monitoredSeriesCreationContext.isImported()) {
                CracCreationReport.removed(String.format("Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesCreationContext.getNativeId(),
                    monitoredSeriesCreationContext.getImportStatus(), monitoredSeriesCreationContext.getImportStatusDetail()), reportNode);
            } else {
                if (monitoredSeriesCreationContext.isAltered()) {
                    CracCreationReport.altered(String.format("Monitored_Series \"%s\" was altered : %s.", monitoredSeriesCreationContext.getNativeId(),
                        monitoredSeriesCreationContext.getImportStatusDetail()), reportNode);
                }
                addToReport(monitoredSeriesCreationContext.getMeasurementCreationContexts(), monitoredSeriesCreationContext.getNativeId(), reportNode);
            }
        }
    }

    private void addToReport(Set<MeasurementCreationContext> measurementCreationContexts, String monitoredSeriesNativeId, ReportNode reportNode) {
        for (MeasurementCreationContext measurementCreationContext : measurementCreationContexts) {
            if (!measurementCreationContext.isImported()) {
                CracCreationReport.removed(String.format("A Measurement in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesNativeId,
                    measurementCreationContext.getImportStatus(), measurementCreationContext.getImportStatusDetail()), reportNode);
            } else {
                for (CnecCreationContext cnecCreationContext : measurementCreationContext.getCnecCreationContexts().values()) {
                    if (!cnecCreationContext.isImported()) {
                        CracCreationReport.removed(String.format("A Cnec in Monitored_Series \"%s\" was not imported: %s. %s.", monitoredSeriesNativeId,
                            cnecCreationContext.getImportStatus(), cnecCreationContext.getImportStatusDetail()), reportNode);
                    }
                }
            }
        }
    }

    private void addToReport(Set<VoltageCnecCreationContext> voltageCnecCreationContexts, ReportNode reportNode) {
        voltageCnecCreationContexts.stream().filter(context -> !context.isImported()).sorted(Comparator.comparing(VoltageCnecCreationContext::getImportStatusDetail)).forEach(context -> {
            String neId = context.getNativeNetworkElementId() != null ? context.getNativeNetworkElementId() : "all";
            String instant = context.getInstantId() != null ? context.getInstantId().toLowerCase() : "all";
            String coName = context.getNativeContingencyName() != null ? context.getNativeContingencyName() : "all";
            CracCreationReport.removed(String.format("VoltageCnec with network element \"%s\", instant \"%s\" and contingency \"%s\" was not imported: %s. %s.", neId, instant, coName, context.getImportStatus(), context.getImportStatusDetail()), reportNode);
        });
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    public void addAngleCnecCreationContext(AngleCnecCreationContext angleCnecCreationContext) {
        this.angleCnecCreationContexts.add(angleCnecCreationContext);
    }

    public Set<AngleCnecCreationContext> getAngleCnecCreationContexts() {
        return new LinkedHashSet<>(angleCnecCreationContexts);
    }

    public AngleCnecCreationContext getAngleCnecCreationContext(String seriesId) {
        return angleCnecCreationContexts.stream().filter(creationContext -> creationContext.getNativeId().equals(seriesId)).findAny().orElse(null);
    }

    public void addVoltageCnecCreationContext(VoltageCnecCreationContext voltageCnecCreationContext) {
        this.voltageCnecCreationContexts.add(voltageCnecCreationContext);
    }

    public Set<VoltageCnecCreationContext> getVoltageCnecCreationContexts() {
        return new LinkedHashSet<>(voltageCnecCreationContexts);
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

    public void setContingencyCreationContexts(Set<CimContingencyCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = new LinkedHashSet<>(contingencyCreationContexts);
    }

    public Set<CimContingencyCreationContext> getContingencyCreationContexts() {
        return new LinkedHashSet<>(contingencyCreationContexts);
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
        this.remedialActionSeriesCreationContexts = new LinkedHashSet<>(remedialActionCreationContexts);
    }

    public Set<RemedialActionSeriesCreationContext> getRemedialActionSeriesCreationContexts() {
        return remedialActionSeriesCreationContexts;
    }

    public RemedialActionSeriesCreationContext getRemedialActionSeriesCreationContext(String seriesId) {
        return remedialActionSeriesCreationContexts.stream().filter(creationContext -> creationContext.getNativeId().equals(seriesId)).findAny().orElse(null);
    }

    public CimContingencyCreationContext getContingencyCreationContextById(String contingencyId) {
        return contingencyCreationContexts.stream().filter(contingencyCreationContext -> contingencyCreationContext.getNativeId().equals(contingencyId)).findAny().orElse(null);
    }

    public CimContingencyCreationContext getContingencyCreationContextByName(String contingencyName) {
        return contingencyCreationContexts.stream().filter(contingencyCreationContext -> contingencyCreationContext.getNativeName().equals(contingencyName)).findAny().orElse(null);
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
