package com.ning.arecibo.util.galaxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.google.inject.Inject;
import com.ning.arecibo.util.Logger;

public class GalaxyCorePicker
{
    private static Logger log = Logger.getLogger(GalaxyCorePicker.class);
    private final GalaxyConfig config;

    @Inject
    public GalaxyCorePicker(GalaxyConfig config)
    {
        this.config = config;
    }

    public List<GalaxyCoreStatus> getCores() throws GalaxyShowWrapperException
    {
        if (config.getGlobalZoneFilter() != null) {
            log.info("using global_zone_filter = '" + config.getGlobalZoneFilter() + "'");
        }

        if (config.getLocalZoneFilter() != null) {
            log.info("using local_zone_filter = '" + config.getLocalZoneFilter() + "'");
        }

        if (config.getCoreTypeFilter() != null) {
            log.info("using core_type_filter = '" + config.getCoreTypeFilter() + "'");
        }

        List<GalaxyCoreStatus> coreStatii = null;

        if (StringUtils.isNotBlank(config.getGalaxyOutputOverrideFile())) {
            log.info("Opening galaxy output file: " + config.getGalaxyOutputOverrideFile());
            InputStream galaxyStream;
            
            try {
                galaxyStream = new FileInputStream(config.getGalaxyOutputOverrideFile());
            }
            catch(IOException ioEx) {
                throw new GalaxyShowWrapperException("Got IOException",ioEx);
            }
            
            Reader galaxyReader = new InputStreamReader(galaxyStream);
            
            // read from file
            coreStatii = GalaxyStatusReader.getCoreStatusList(galaxyReader);
        }
        else {
            if (config.getGalaxyCommandPath() != null) {
                log.info("using galaxy_command_path = '" + config.getGalaxyCommandPath() + "'");
            }

            for (String gonsoleUrl : config.getGonsoleUrls()) {
                // call galaxy as a system call
                log.info("using gonsole_url = '" + gonsoleUrl + "'");

                List<GalaxyCoreStatus> gonsoleCoreStatii = GalaxyShowWrapper.getCoreStatusList(gonsoleUrl,
                                                                                               config.getGalaxyCommandPath(),
                                                                                               config.getGalaxyCommandTimeout(), 
                                                                                               config.getCoreTypeFilter(),
                                                                                               config.getGlobalZoneFilter(),
                                                                                               config.getLocalZoneFilter());
                
                log.info("found " + gonsoleCoreStatii.size() + " cores for gonsole_url = '" + gonsoleUrl + "'");
                
                if(coreStatii == null)
                    coreStatii = gonsoleCoreStatii;
                else
                    coreStatii.addAll(gonsoleCoreStatii);
            }
        }

        if (coreStatii == null) {
            return new ArrayList<GalaxyCoreStatus>();
        }

        return coreStatii;
    }
}
