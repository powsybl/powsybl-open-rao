package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CnecResultExtension extends AbstractResultExtension<BranchCnec, CnecResult> {

    @Override
    public String getName() {
        return "CnecResultExtension";
    }
}
