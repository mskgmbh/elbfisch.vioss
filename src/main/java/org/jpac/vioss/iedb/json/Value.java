package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Value {
    @JsonProperty("id")
    private String id;

    @JsonProperty("qc")
    private Integer qualityCode;

    @JsonProperty("ts")
    private String timestamp;

    @JsonFormat(with = {
            JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
            JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
    })
    @JsonProperty("val")
    private List<Object> value;
}