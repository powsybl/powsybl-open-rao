package com.farao_community.farao.data.crac_api;

public interface RemedialActionAdder<T extends RemedialActionAdder> extends IdentifiableAdder<T> {

    T withOperator(String operator);

}
