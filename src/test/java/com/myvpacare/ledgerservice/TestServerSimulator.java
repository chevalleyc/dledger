package com.myvpacare.ledgerservice;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.vehr.Launcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public abstract class TestServerSimulator {

    protected Launcher launcher = null;
    protected RunTimeSingleton global;
    protected String resourcesRootPath;
    protected final String httpPort = "8080";
    protected static String hostname = "localhost";

    @Before
    public void setUp() throws Exception {
        launcher = new Launcher();

        launcher.start(new String[]{
                "-propertyFile", "src/test/resources/services.properties",
                "-java_util_logging_config_file", "src/test/resources/logging.properties",
                "-servicesFile", "src/test/resources/services.xml",
                "-dialect", "EHRSCAPE",
//                "-server_port", httpPort,
//                "-server_host", hostname,
                "-debug", "true"
        });

        setResourcesRootPath();

        global = launcher.getGlobal();
    }


    protected void setResourcesRootPath() {
        resourcesRootPath = getClass()
                .getClassLoader()
                .getResource(".")
                .getFile();
    }

    public String sessionId(String responseBody) {
        Gson json = new GsonBuilder().create();
        Map<String, Object> responseMap = json.fromJson(responseBody, Map.class);
        return (String) responseMap.get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));
    }

    public byte[] readXMLNoBOM(String filePath) throws IOException {
        //read in a template into a string
        Path path = Paths.get(filePath);
        String readin = FileUtils.readFileToString(path.toFile(), "UTF-8");
        //start at index == 1 to eliminate any residual XML BOM (byte order mark)!!! see http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
        return readin.substring(readin.indexOf("<")).getBytes();
    }
}
