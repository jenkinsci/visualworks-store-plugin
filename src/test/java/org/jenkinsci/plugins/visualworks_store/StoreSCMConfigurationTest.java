/*
 * The MIT License
 *
 * Copyright (c) 2013. Randy Coulman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.visualworks_store;

import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.JenkinsRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StoreSCMConfigurationTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testGlobalConfigurationRoundtrip() throws Exception {
        StoreSCM.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(StoreSCM.DescriptorImpl.class);

        descriptor.setStoreScripts(new StoreScript("7.7.1", "/path/to/script-7.7.1"),
                new StoreScript("7.9.1", "/path/to/script-7.9.1"));

        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        StoreScript[] scripts = descriptor.getStoreScripts();
        assertEquals("installation count", 2, scripts.length);
        assertEquals("first script name", "7.7.1", scripts[0].getName());
        assertEquals("first script path", "/path/to/script-7.7.1", scripts[0].getPath());
        assertEquals("second script name", "7.9.1", scripts[1].getName());
        assertEquals("second script path", "/path/to/script-7.9.1", scripts[1].getPath());
    }

    @Test
    public void testBasicConfigurationRoundtrip() throws Exception {
        StoreSCM.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(StoreSCM.DescriptorImpl.class);
        descriptor.setStoreScripts(new StoreScript("theScript", "path"));

        List<PundleSpec> pundleSpecs = onePundle();
        StoreSCM scm = new StoreSCM("theScript", "Repo", pundleSpecs, "\\d+", "Integrated", false, "");
        StoreSCM loaded = doRoundtripConfiguration(scm);

        assertEquals("script name", "theScript", loaded.getScriptName());
        assertEquals("repositoryName", "Repo", loaded.getRepositoryName());
        assertEquals("pundles", pundleSpecs, loaded.getPundles());
        assertEquals("versionRegex", "\\d+", loaded.getVersionRegex());
        assertEquals("minimumBlessingLevel", "Integrated", loaded.getMinimumBlessingLevel());
        assertFalse("generateParcelBuilderInputFile", loaded.isGenerateParcelBuilderInputFile());
        assertEquals("parcelBuilderInputFilename", "", loaded.getParcelBuilderInputFilename());
    }

    @Test
    public void testConfigurationRoundtripWithMultiplePundles() throws Exception {
        StoreSCM.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(StoreSCM.DescriptorImpl.class);
        descriptor.setStoreScripts(new StoreScript("theScript", "path"));

        List<PundleSpec> pundleSpecs = Arrays.asList(new PundleSpec(PundleType.PACKAGE, "SomePackage"),
                new PundleSpec(PundleType.BUNDLE, "SomeBundle"));
        StoreSCM scm = new StoreSCM("theScript", "Repo", pundleSpecs, "\\d+", "Integrated", false, "");
        StoreSCM loaded = doRoundtripConfiguration(scm);

        assertEquals("pundles", pundleSpecs, loaded.getPundles());
    }

    @Test
    public void testConfigurationRoundtripWithParcelBuilderFile() throws Exception {
        StoreSCM scm = new StoreSCM("script", "Repo", onePundle(), "\\d+", "Integrated", true, "theFilename");
        StoreSCM loaded = doRoundtripConfiguration(scm);

        assertTrue("generateParcelBuilderInputFile", loaded.isGenerateParcelBuilderInputFile());
        assertEquals("parcelBuilderInputFilename", "theFilename", loaded.getParcelBuilderInputFilename());
    }

    @Test
    public void testLookupStoreScript() {
        StoreSCM.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(StoreSCM.DescriptorImpl.class);
        final StoreScript script = new StoreScript("otherScript", "otherPath");
        descriptor.setStoreScripts(new StoreScript("theScript", "path"), script);

        StoreSCM scm = new StoreSCM("otherScript", "Repo", onePundle(), "\\d+", "Development", false, "");

        assertEquals(script, scm.getStoreScript());
    }

    private StoreSCM doRoundtripConfiguration(StoreSCM original) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.setScm(original);

        j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));

        return (StoreSCM) p.getScm();
    }

    private List<PundleSpec> onePundle() {
        return Arrays.asList(new PundleSpec(PundleType.PACKAGE, "SomePackage"));
    }
}
