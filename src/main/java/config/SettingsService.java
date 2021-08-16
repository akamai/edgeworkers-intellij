package config;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;

@State(name = "EdgeWorkersConfig", storages = {@Storage("edgeWorkersConfig.xml")})
public class SettingsService implements PersistentStateComponent<EdgeWorkersConfig> {

    private EdgeWorkersConfig config = new EdgeWorkersConfig();

    public static SettingsService getInstance() {
        return ServiceManager.getService(SettingsService.class);
    }

    @Override
    public @Nullable EdgeWorkersConfig getState() {
        return config;
    }

    @Override
    public void loadState(@NotNull EdgeWorkersConfig state) {
        if(state.getEdgercFilePath().isEmpty()){
            File file = new File(System.getProperty("user.home"));
            //default edgerc file path
            String path = Paths.get(file.getAbsolutePath(),"/.edgerc").toString();
            state.setEdgercFilePath(path);
        }
        XmlSerializerUtil.copyBean(state, config);
    }

    public void updateConfig(EdgeWorkersConfig edgeWorkersConfig){
        config = edgeWorkersConfig;
    }

}
