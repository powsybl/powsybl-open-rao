package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

public enum CsaProfileKeywords {
    ASSESSED_ELEMENT("AE"),
    CONTINGENCY("CO"),
    EQUIPMENT_RELIABILITY("ER"),
    REMEDIAL_ACTION("RA"),
    REMEDIAL_ACTION_SCHEDULE("RAS"),
    STEADY_STATE_INSTRUCTION("SSI"),
    STATE_INSTRUCTION_SCHEDULE("SIS");

    private final String keyword;

    CsaProfileKeywords(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return keyword;
    }
}

