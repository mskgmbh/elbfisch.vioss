package org.jpac.vioss.iedb.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) 
@Data
public class OpcUaConnectorConfig {
    @JsonProperty("applicationName")
    private String applicationName;

    @JsonProperty("connections")
    private List<Connection> connections;

    @JsonProperty("hashVersion")
    private long hashVersion;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("versionMajor")
    private int versionMajor;

    @JsonProperty("versionMinor")
    private int versionMinor;

    @JsonProperty("versionPatch")
    private int versionPatch;
    
    @JsonProperty("seq")
    private int seq;

    @JsonProperty("statustopic")
    private String statusTopic;
}