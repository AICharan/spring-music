package org.cloudfoundry.samples.music.web;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.cloudfoundry.samples.music.domain.ApplicationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InfoController {

    private final CfEnv cfEnv;
    private final Environment springEnvironment;

    @Autowired
    public InfoController(Environment springEnvironment) {
        this.springEnvironment = springEnvironment;
        this.cfEnv = new CfEnv();
    }

    @GetMapping("/appinfo")
    public ApplicationInfo info() {
        return new ApplicationInfo(springEnvironment.getActiveProfiles(), getServiceNames());
    }

    @GetMapping("/service")
    public List<CfService> showServiceInfo() {
        return cfEnv.findAllServices();
    }

    private String[] getServiceNames() {
        return cfEnv.findAllServices().stream()
                .map(CfService::getName)
                .toArray(String[]::new);
    }
}
