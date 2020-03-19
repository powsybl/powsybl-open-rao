package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracResultExtension extends AbstractResultExtension<Crac, CracResult> {

    @Override
    public String getName() {
        return "CracResultExtension";
    }
}
