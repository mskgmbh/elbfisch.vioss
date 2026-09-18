package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ValueList {
    @JsonProperty("seq")
    private int sequence;

    @JsonProperty("vals")
    private List<Value> values;
}
