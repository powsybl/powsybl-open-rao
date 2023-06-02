/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package main.java.com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import main.java.com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;

import java.time.OffsetDateTime;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCracCreator implements CracCreator<CsaProfileCrac, CsaProfileCracCreationContext> {
    @Override
    public String getNativeCracFormat() {
        return null;
    }

    @Override
    public CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, com.powsybl.iidm.network.Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        return null;
    }
}
