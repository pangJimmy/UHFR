package com.handheld.uhfr;

import com.gg.reader.api.protocol.gx.ParamEpcFilter;

public class CusParamFilter {
    ParamEpcFilter filter;
    boolean matching;

    public CusParamFilter(ParamEpcFilter filter, boolean matching) {
        this.filter = filter;
        this.matching = matching;
    }

    public ParamEpcFilter getFilter() {
        return filter;
    }

    public void setFilter(ParamEpcFilter filter) {
        this.filter = filter;
    }

    public boolean isMatching() {
        return matching;
    }

    public void setMatching(boolean matching) {
        this.matching = matching;
    }

    @Override
    public String toString() {
        return "CusParamFilter{" +
                "filter=" + filter +
                ", matching=" + matching +
                '}';
    }

}
