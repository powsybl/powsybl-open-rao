/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationReport;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class FbConstraintCreationContext implements UcteCracCreationContext {

    private Crac crac;
    private boolean isCreationSuccessful;
    private final OffsetDateTime timeStamp;
    private final String networkName;
    private final Map<String, CriticalBranchCreationContext> criticalBranchCreationContexts;
    private final Map<String, ElementaryCreationContext> complexVariantCreationContexts;
    private final CracCreationReport creationReport;

    @Override
    public boolean isCreationSuccessful() {
        return isCreationSuccessful;
    }

    @Override
    public Crac getCrac() {
        return crac;
    }

    @Override
    public List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts() {
        return new ArrayList<>(criticalBranchCreationContexts.values());
    }

    @Override
    public List<? extends ElementaryCreationContext> getRemedialActionCreationContexts() {
        return new ArrayList<>(complexVariantCreationContexts.values());
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }

    @Override
    public CriticalBranchCreationContext getBranchCnecCreationContext(String criticalBranchId) {
        return criticalBranchCreationContexts.get(criticalBranchId);
    }

    @Override
    public ElementaryCreationContext getRemedialActionCreationContext(String complexVariantId) {
        return complexVariantCreationContexts.get(complexVariantId);
    }

    void addCriticalBranchCreationContext(CriticalBranchCreationContext cbcc) {
        criticalBranchCreationContexts.put(cbcc.getNativeObjectId(), cbcc);
    }

    void addComplexVariantCreationContext(ElementaryCreationContext context) {
        complexVariantCreationContexts.put(context.getNativeObjectId(), context);
    }

    FbConstraintCreationContext(OffsetDateTime timeStamp, String networkName) {
        this.criticalBranchCreationContexts = new HashMap<>();
        this.complexVariantCreationContexts = new HashMap<>();
        this.isCreationSuccessful = false;
        this.timeStamp = timeStamp;
        this.networkName = networkName;
        this.creationReport = new CracCreationReport();
    }

    protected FbConstraintCreationContext(FbConstraintCreationContext toCopy) {
        this.criticalBranchCreationContexts = new HashMap<>(toCopy.criticalBranchCreationContexts);
        this.complexVariantCreationContexts = new HashMap<>(toCopy.complexVariantCreationContexts);
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.timeStamp = toCopy.timeStamp;
        this.networkName = toCopy.networkName;
        this.creationReport = new CracCreationReport(toCopy.creationReport);
        this.crac = toCopy.crac;
    }

    FbConstraintCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    FbConstraintCreationContext creationSucess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
        return this;
    }

    void buildCreationReport() {
        addToReport(criticalBranchCreationContexts.values(), "Critical branch");
        addToReport(complexVariantCreationContexts.values(), "Remedial action");
    }

    private void addToReport(Collection<? extends ElementaryCreationContext> contexts, String nativeTypeIdentifier) {
        contexts.stream().filter(ElementaryCreationContext::isAltered).forEach(context ->
            creationReport.altered(String.format("%s \"%s\" was modified: %s. ", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatusDetail()))
        );
        contexts.stream().filter(context -> !context.isImported()).forEach(context ->
            creationReport.removed(String.format("%s \"%s\" was not imported: %s. %s.", nativeTypeIdentifier, context.getNativeObjectId(), context.getImportStatus(), context.getImportStatusDetail()))
        );
    }
}


