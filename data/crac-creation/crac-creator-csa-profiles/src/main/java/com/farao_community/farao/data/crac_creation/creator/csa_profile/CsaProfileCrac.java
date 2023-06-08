/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile;

import com.farao_community.farao.data.native_crac_api.NativeCrac;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCrac implements NativeCrac {
/*
    private final CRACDocumentType cracDocumentType;

    public CsaProfileCrac(CRACDocumentType cracDocumentType) {
        this.cracDocumentType = cracDocumentType;
    }*/

    @Override
    public String getFormat() {
        return "CsaProfileCrac";
    }

    /*public CRACDocumentType getCracDocument() {
        return cracDocumentType;
    }*/
}
