package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DataPointDefinition {
    @JsonProperty("accessMode")
    private String accessMode; // e.g., "rw"

    @JsonProperty("acquisitionCycleInMs")
    private int acquisitionCycleInMs;

    @JsonProperty("acquisitionMode")
    private String acquisitionMode; // e.g., "CyclicOnChange"

    @JsonProperty("arrayDimensions")
    private List<Integer> arrayDimensions; // e.g., [0]

    @JsonProperty("dataType")
    private String dataType; // e.g., "Byte", "Int", "Bool"

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name; // e.g., "msk/automation/byteArray0"

    @JsonProperty("valueRank")
    private Integer valueRank; // Optional, e.g., 1
}
