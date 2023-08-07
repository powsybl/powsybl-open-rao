/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionCreationContext;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCracCreationContext implements CracCreationContext {

    private Crac crac;

    private boolean isCreationSuccessful;

    private Set<CsaProfileContingencyCreationContext> contingencyCreationContexts;

    private Set<CsaProfileRemedialActionCreationContext> remedialActionCreationContext;

    private final CracCreationReport creationReport;

    private final OffsetDateTime timeStamp;

    private final String networkName;

    CsaProfileCracCreationContext(Crac crac, OffsetDateTime timeStamp, String networkName) {
        this.crac = crac;
        creationReport = new CracCreationReport();
        this.timeStamp = timeStamp;
        this.networkName = networkName;
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

    public void setContingencyCreationContexts(Set<CsaProfileContingencyCreationContext> contingencyCreationContexts) {
        this.contingencyCreationContexts = contingencyCreationContexts.stream().collect(Collectors.toSet());
    }

    public Set<CsaProfileContingencyCreationContext> getContingencyCreationContexts() {
        return this.contingencyCreationContexts.stream().collect(Collectors.toSet());
    }

    public Set<CsaProfileRemedialActionCreationContext> getRemedialActionCreationContext() {
        return remedialActionCreationContext;
    }

    public void setRemedialActionCreationContext(Set<CsaProfileRemedialActionCreationContext> remedialActionCreationContext) {
        this.remedialActionCreationContext = remedialActionCreationContext;
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
}
