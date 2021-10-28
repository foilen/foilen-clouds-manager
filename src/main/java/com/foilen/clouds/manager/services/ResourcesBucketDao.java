/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.io.File;

import com.foilen.clouds.manager.services.model.ResourcesBucket;
import com.foilen.smalltools.db.AbstractSingleYamlFileDao;
import com.foilen.smalltools.tools.DirectoryTools;

public class ResourcesBucketDao extends AbstractSingleYamlFileDao<ResourcesBucket> {

    private File dbFile;
    private File stagingFile;

    public ResourcesBucketDao(String path) {
        this.dbFile = new File(path);
        this.stagingFile = new File(dbFile.getAbsolutePath() + "_tmp");
        DirectoryTools.createPathToFile(path);
    }

    @Override
    protected File getFinalFile() {
        return dbFile;
    }

    @Override
    protected File getStagingFile() {
        return stagingFile;
    }

    @Override
    protected Class<ResourcesBucket> getType() {
        return ResourcesBucket.class;
    }

}
