package org.cloudfoundry.samples.music.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationInfo {
    private String[] profiles;
    private String[] services;
}
