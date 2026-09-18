package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Connection {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type; // e.g., "OPCUA"

    @JsonProperty("dataPoints")
    private List<DataPoint> dataPoints;
}
