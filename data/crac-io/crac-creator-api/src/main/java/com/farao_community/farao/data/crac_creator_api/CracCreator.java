package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.raw_crac_api.RawCrac;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

public interface CracCreator<T extends RawCrac> {

    CracCreationResult<T> createCrac(T rawCrac, Network network, OffsetDateTime offsetDateTime);

}
