/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cse;

import com.powsybl.openrao.data.crac.io.cse.criticalbranch.CseCriticalBranchCreationContext;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationReport;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCracCreationContext implements UcteCracCreationContext {

    private Crac crac;
    private boolean isCreationSuccessful;
    private CracCreationReport creationReport;
    private Map<String, CseCriticalBranchCreationContext> criticalBranchCreationContexts = new HashMap<>();
    private Map<String, CseCriticalBranchCreationContext> monitoredElementCreationContexts = new HashMap<>();
    private Map<String, ElementaryCreationContext> outageCreationContexts = new HashMap<>();
    private Map<String, ElementaryCreationContext> remedialActionCreationContexts = new HashMap<>();
    private final String networkName;

    public CseCracCreationContext(Crac crac, String networkName) {
        this.crac = crac;
        this.networkName = networkName;
        creationReport = new CracCreationReport();
    }

    protected CseCracCreationContext(CseCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.creationReport = new CracCreationReport(toCopy.creationReport);
        this.criticalBranchCreationContexts = new HashMap<>(toCopy.criticalBranchCreationContexts);
        this.monitoredElementCreationContexts = new HashMap<>(toCopy.monitoredElementCreationContexts);
        this.outageCreationContexts = new HashMap<>(toCopy.outageCreationContexts);
        this.remedialActionCreationContexts = new HashMap<>(toCopy.remedialActionCreationContexts);
        this.networkName = toCopy.networkName;
    }

    @Override
    public List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts() {
        ArrayList<CseCriticalBranchCreationContext> list = new ArrayList<>(criticalBranchCreationContexts.values());
        list.addAll(monitoredElementCreationContexts.values());
        return list;
    }

    @Override
    public List<? extends ElementaryCreationContext> getRemedialActionCreationContexts() {
        return new ArrayList<>(remedialActionCreationContexts.values());
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    @Override
    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return null;
    }

    void buildCreationReport() {
        addToReport(outageCreationContexts.values(), "Outage");
        addToReport(criticalBranchCreationContexts.values(), "Critical branch");
        addToReport(monitoredElementCreationContexts.values(), "Monitored element");
        addToReport(remedialActionCreationContexts.values(), "Remedial action");
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered)
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
            .forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported())
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeObjectId))
            .forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }

    public void addCriticalBranchCreationContext(CseCriticalBranchCreationContext cseCriticalBranchCreationContext) {
        this.criticalBranchCreationContexts.put(cseCriticalBranchCreationContext.getNativeObjectId(), cseCriticalBranchCreationContext);
    }

    public void addMonitoredElementCreationContext(CseCriticalBranchCreationContext cseCriticalBranchCreationContext) {
        this.monitoredElementCreationContexts.put(cseCriticalBranchCreationContext.getNativeObjectId(), cseCriticalBranchCreationContext);
    }

    @Override
    public BranchCnecCreationContext getBranchCnecCreationContext(String nativeCnecId) {
        if (criticalBranchCreationContexts.containsKey(nativeCnecId)) {
            return criticalBranchCreationContexts.get(nativeCnecId);
        } else {
            return monitoredElementCreationContexts.get(nativeCnecId);
        }
    }

    public ElementaryCreationContext getOutageCreationContext(String outageName) {
        return outageCreationContexts.get(outageName);
    }

    public List<ElementaryCreationContext> getOutageCreationContexts() {
        return new ArrayList<>(outageCreationContexts.values());
    }

    public void addOutageCreationContext(ElementaryCreationContext cseOutageCreationContext) {
        this.outageCreationContexts.put(cseOutageCreationContext.getNativeObjectId(), cseOutageCreationContext);
    }

    @Override
    public ElementaryCreationContext getRemedialActionCreationContext(String raName) {
        return remedialActionCreationContexts.get(raName);
    }

    public void addRemedialActionCreationContext(ElementaryCreationContext remedialActionCreationContext) {
        this.remedialActionCreationContexts.put(remedialActionCreationContext.getNativeObjectId(), remedialActionCreationContext);
    }

    CseCracCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    CseCracCreationContext creationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.creationReport.addSuccessfulImportMessage(crac);
        this.crac = crac;
        return this;
    }
}
