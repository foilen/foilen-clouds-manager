/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.cli.services;

import org.junit.Assert;
import org.junit.Test;

public class SshServiceTest {

    @Test
    public void testTrimSlashes() {
        Assert.assertEquals("", SshService.trimSlashes(null));
        Assert.assertEquals("", SshService.trimSlashes("////"));
        Assert.assertEquals("sub", SshService.trimSlashes("sub"));
        Assert.assertEquals("sub/1", SshService.trimSlashes("sub/1"));
        Assert.assertEquals("sub/1", SshService.trimSlashes("/sub/1/"));
        Assert.assertEquals("sub/1", SshService.trimSlashes("///sub/1///"));
    }

}
