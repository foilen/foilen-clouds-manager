/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.model.OnlineFileDetails;
import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class MavenCentralService extends AbstractBasics {

    public OnlineFileDetails getLatestVersion(String packageName) {
        try {
            // Get the version
            Document doc = Jsoup.connect("https://repo1.maven.org/maven2/com/foilen/" + packageName + "/").get();
            Elements links = doc.select("a");
            String version = links.stream() //
                    .map(it -> it.text().replace("/", "")) //
                    .map(it -> it.split("\\.")) //
                    .filter(it -> it.length == 3) //
                    .map(it -> {
                        try {
                            return new int[]{Integer.parseInt(it[0]), Integer.parseInt(it[1]), Integer.parseInt(it[2])};
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }) //
                    .filter(Objects::nonNull) //
                    .sorted((a, b) -> ComparisonChain.start() //
                            .compare(b[0], a[0]) //
                            .compare(b[1], a[1]) //
                            .compare(b[2], a[2]) //
                            .result()) //
                    .map(it -> "" + it[0] + "." + it[1] + "." + it[2]) //
                    .findFirst().get(); //

            // Get the jar
            String jarUrl = "https://repo1.maven.org/maven2/com/foilen/" + packageName + "/" + version + "/" + packageName + "-" + version + ".jar";

            return new OnlineFileDetails()
                    .setJarUrl(jarUrl)
                    .setVersion(version);
        } catch (IOException e) {
            throw new CliException("Problem getting the folder", e);
        }

    }

}
