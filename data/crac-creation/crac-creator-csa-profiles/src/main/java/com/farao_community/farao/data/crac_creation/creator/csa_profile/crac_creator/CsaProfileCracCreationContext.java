/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package main.java.com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationReport;

import java.time.OffsetDateTime;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCracCreationContext implements CracCreationContext {
    @Override
    public boolean isCreationSuccessful() {
        return false;
    }

    @Override
    public com.farao_community.farao.data.crac_api.Crac getCrac() {
        return null;
    }

    @Override
    public OffsetDateTime getTimeStamp() {
        return null;
    }

    @Override
    public String getNetworkName() {
        return null;
    }

    @Override
    public CracCreationReport getCreationReport() {
        return null;
    }
}
