package config;

import kotlin.jvm.Transient;

import java.io.Serializable;

public class EdgeWorkersConfig implements Serializable {

    private String edgercSectionName;

    private String edgercFilePath;

    private String accountKey;

    public String getEdgercSectionName() {
        return edgercSectionName;
    }

    public void setEdgercSectionName(String edgercSectionName) {
        this.edgercSectionName = edgercSectionName;
    }

    public String getEdgercFilePath() {
        return edgercFilePath;
    }

    public void setEdgercFilePath(String edgercFilePath) {
        this.edgercFilePath = edgercFilePath;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

}
