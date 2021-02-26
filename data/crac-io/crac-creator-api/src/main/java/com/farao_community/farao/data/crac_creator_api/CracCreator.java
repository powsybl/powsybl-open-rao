package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.raw_crac_api.RawCrac;
import com.powsybl.iidm.network.Network;

public abstract class CracCreator<T extends RawCrac> {
    
    abstract Crac createCrac(T rawCrac, Network network);

}
