package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CnecResultExtension extends AbstractResultExtension<FlowCnec, CnecResult> {

    @Override
    public String getName() {
        return "CnecResultExtension";
    }
}
