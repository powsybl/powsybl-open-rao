/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.api.ElementaryCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.RemedialActionCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.StandardCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.remedial_action.CseRemedialActionCreationContext;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCracCreationContext implements StandardCracCreationContext {

    private Crac crac;
    private boolean isCreationSuccessful;
    private CracCreationReport creationReport;
    private Map<String, CseCriticalBranchCreationContext> criticalBranchCreationContexts = new HashMap<>();
    private Map<String, CseOutageCreationContext> outageCreationContexts = new HashMap<>();
    private Map<String, CseRemedialActionCreationContext> remedialActionCreationContexts = new HashMap<>();
    private final OffsetDateTime timestamp;
    private final String networkName;

    public CseCracCreationContext(Crac crac, OffsetDateTime timestamp, String networkName) {
        this.crac = crac;
        this.timestamp = timestamp;
        this.networkName = networkName;
        creationReport = new CracCreationReport();
    }

    protected CseCracCreationContext(CseCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.creationReport = new CracCreationReport(toCopy.creationReport);
        this.criticalBranchCreationContexts = new HashMap<>(toCopy.criticalBranchCreationContexts);
        this.outageCreationContexts = new HashMap<>(toCopy.outageCreationContexts);
        this.remedialActionCreationContexts = new HashMap<>(toCopy.remedialActionCreationContexts);
        this.timestamp = toCopy.timestamp;
        this.networkName = toCopy.networkName;
    }

    @Override
    public List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts() {
        return new ArrayList<>(criticalBranchCreationContexts.values());
    }

    @Override
    public List<? extends RemedialActionCreationContext> getRemedialActionCreationContexts() {
        return new ArrayList<>(remedialActionCreationContexts.values());
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return timestamp;
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

    public void buildCreationReport() {
        addToReport(outageCreationContexts.values(), "Outage");
        addToReport(criticalBranchCreationContexts.values(), "Critical branch");
        addToReport(remedialActionCreationContexts.values(), "Remedial action");
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered)
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeId))
            .forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported())
            .sorted(Comparator.comparing(ElementaryCreationContext::getNativeId))
            .forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }

    public void addCriticalBranchCreationContext(CseCriticalBranchCreationContext cseCriticalBranchCreationContext) {
        this.criticalBranchCreationContexts.put(cseCriticalBranchCreationContext.getNativeId(), cseCriticalBranchCreationContext);
    }

    @Override
    public BranchCnecCreationContext getBranchCnecCreationContext(String nativeCnecId) {
        return criticalBranchCreationContexts.get(nativeCnecId);
    }

    public CseOutageCreationContext getOutageCreationContext(String outageName) {
        return outageCreationContexts.get(outageName);
    }

    public List<CseOutageCreationContext> getOutageCreationContexts() {
        return new ArrayList<>(outageCreationContexts.values());
    }

    public void addOutageCreationContext(CseOutageCreationContext cseOutageCreationContext) {
        this.outageCreationContexts.put(cseOutageCreationContext.getNativeId(), cseOutageCreationContext);
    }

    @Override
    public RemedialActionCreationContext getRemedialActionCreationContext(String raName) {
        return remedialActionCreationContexts.get(raName);
    }

    public void addRemedialActionCreationContext(CseRemedialActionCreationContext remedialActionCreationContext) {
        this.remedialActionCreationContexts.put(remedialActionCreationContext.getNativeId(), remedialActionCreationContext);
    }

    CseCracCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    CseCracCreationContext creationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
        return this;
    }
}
