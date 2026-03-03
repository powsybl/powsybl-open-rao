/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.CracCreationReport;
import com.powsybl.openrao.data.crac.api.Instant;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkCracCreationContext implements CracCreationContext {
    private final Crac crac;
    private boolean isCreationSuccessful;
    private final CracCreationReport creationReport;
    private final String networkName;
    private final Map<Instant, Set<String>> injectionsUsedInActions = new HashMap<>();

    public NetworkCracCreationContext(Crac crac, String networkName) {
        this.crac = crac;
        this.networkName = networkName;
        this.creationReport = new CracCreationReport();
    }

    public void setCreationSuccessful(boolean creationSuccessful) {
        isCreationSuccessful = creationSuccessful;
    }

    void addInjectionUsedInAction(Instant instant, String injectionId) {
        injectionsUsedInActions.computeIfAbsent(instant, k -> new HashSet<>()).add(injectionId);
    }

    boolean isInjectionUsedInAction(Instant instant, String injectionId) {
        return injectionsUsedInActions.containsKey(instant) && injectionsUsedInActions.get(instant).contains(injectionId);
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
        return crac.getTimestamp().orElseThrow();
    }

    @Override
    public String getNetworkName() {
        return networkName;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return creationReport;
    }
}
