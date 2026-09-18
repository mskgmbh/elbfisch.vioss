package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DataPoint {
    @JsonProperty("name")
    private String name;

    @JsonProperty("pubTopic")
    private String pubTopic;

    @JsonProperty("publishType")
    private String publishType; // e.g., "bulk"

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("dataPointDefinitions")
    private List<DataPointDefinition> dataPointDefinitions;
}