package com.farao_community.farao.data.crac_creator_api.std_creation_context;

public class NativeBranch {

    private String from;
    private String to;
    private String suffix;

    public NativeBranch(String from, String to, String suffix) {
        this.from = from;
        this.to = to;
        this.suffix = suffix;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSuffix() {
        return suffix;
    }
}
