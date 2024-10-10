package com.powsybl.openrao.data.raoresult.nc;

public enum Namespace {
    CIM("cim", "http://iec.ch/TC57/CIM100#"),
    DCAT("dcat", "http://www.w3.org/ns/dcat#"),
    MD("md", "http://iec.ch/TC57/61970-552/ModelDescription/1#"),
    NC("nc", "http://entsoe.eu/ns/nc#");

    private final String keyword;
    private final String uri;

    Namespace(String keyword, String uri) {
        this.keyword = keyword;
        this.uri = uri;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getUri() {
        return uri;
    }
}
