/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.CracCreationReport;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
// TODO : make all setters private package
public class CsaProfileCracCreationContext implements CracCreationContext {

    private Crac crac;

    private boolean isCreationSuccessful;

    private Set<ElementaryCreationContext> contingencyCreationContexts;

    private Set<ElementaryCreationContext> remedialActionCreationContexts;

    private Set<ElementaryCreationContext> cnecCreationContexts;

    private CracCreationReport creationReport;

    private final OffsetDateTime timeStamp;

    private final String networkName;

    CsaProfileCracCreationContext(Crac crac, OffsetDateTime timeStamp, String networkName) {
        this.crac = crac;
        this.creationReport = new CracCreationReport();
        this.timeStamp = timeStamp;
        this.networkName = networkName;
    }

    protected CsaProfileCracCreationContext(CsaProfileCracCreationContext toCopy) {
        this.crac = toCopy.crac;
        this.creationReport = toCopy.creationReport;
        this.timeStamp = toCopy.timeStamp;
        this.networkName = toCopy.networkName;
        this.isCreationSuccessful = toCopy.isCreationSuccessful;
        this.contingencyCreationContexts = new HashSet<>(toCopy.contingencyCreationContexts);
        this.remedialActionCreationContexts = new HashSet<>(toCopy.remedialActionCreationContexts);
        this.cnecCreationContexts = new HashSet<>(toCopy.cnecCreationContexts);
    }

    @Override
    public boolean isCreationSuccessful() {
        return this.isCreationSuccessful;
    }

    @Override
    public Crac getCrac() {
        return this.crac;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String getNetworkName() {
        return this.networkName;
    }

    public void setContingencyCreationContexts(Set<ElementaryCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = new HashSet<>(contingencyCreationContexts);
    }

    public Set<ElementaryCreationContext> getContingencyCreationContexts() {
        return new HashSet<>(this.contingencyCreationContexts);
    }

    public Set<ElementaryCreationContext> getRemedialActionCreationContexts() {
        return new HashSet<>(remedialActionCreationContexts);
    }

    public ElementaryCreationContext getRemedialActionCreationContext(String nativeId) {
        return remedialActionCreationContexts.stream().filter(rac -> rac.getNativeObjectId().equals(nativeId)).findFirst().orElse(null);
    }

    public void setRemedialActionCreationContexts(Set<ElementaryCreationContext> remedialActionCreationContexts) {
        this.remedialActionCreationContexts = remedialActionCreationContexts;
    }

    public void setCnecCreationContexts(Set<ElementaryCreationContext> cnecCreationContexts) {
        this.cnecCreationContexts = new HashSet<>(cnecCreationContexts);
    }

    public Set<ElementaryCreationContext> getCnecCreationContexts() {
        return new HashSet<>(this.cnecCreationContexts);
    }

    @Override
    public CracCreationReport getCreationReport() {
        return this.creationReport;
    }

    CsaProfileCracCreationContext creationFailure() {
        this.isCreationSuccessful = false;
        this.crac = null;
        return this;
    }

    CsaProfileCracCreationContext creationSuccess(Crac crac) {
        this.isCreationSuccessful = true;
        this.crac = crac;
        return this;
    }

    public void buildCreationReport() {
        creationReport = new CracCreationReport();
        addToReport(contingencyCreationContexts, "Contingencies");
        addToReport(cnecCreationContexts, "Cnecs");
        addToReport(remedialActionCreationContexts, "RemedialActions");
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
