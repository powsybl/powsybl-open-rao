/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.parameters;

import com.powsybl.open_rao.data.crac_creation.creator.api.parameters.AbstractAlignedRaCracCreationParameters;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractAlignedRaCracCreationParameters {
    private boolean useCnecGeographicalFilter = false;

    @Override
    public String getName() {
        return "CsaCracCreatorParameters";
    }

    public void setUseCnecGeographicalFilter(boolean useCnecGeographicalFilter) {
        this.useCnecGeographicalFilter = useCnecGeographicalFilter;
    }

    public boolean getUseCnecGeographicalFilter() {
        return useCnecGeographicalFilter;
    }
}
