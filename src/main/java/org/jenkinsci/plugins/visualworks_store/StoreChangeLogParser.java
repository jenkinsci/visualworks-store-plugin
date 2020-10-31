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

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.util.IOException2;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreChangeLogParser extends ChangeLogParser {
    private List<StoreChangeLogEntry> entries;
    private Map<String, StoreChangeLogEntry> blessingMap;

    @Override
    public StoreChangeLogSet parse(AbstractBuild build, File file) throws IOException, SAXException {
        entries = new ArrayList<StoreChangeLogEntry>();
        blessingMap = new HashMap<String, StoreChangeLogEntry>();

        SAXReader reader = new SAXReader();
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        try {
            Document document = reader.read(file);
            Element root = document.getRootElement();
            for (Object eachPundle : root.elements()) {
                Element pundle = (Element) eachPundle;

                PundleType pundleType = PundleType.named(pundle.getName());
                if (pundleType == null) continue;

                List blessings = pundle.elements("blessing");

                ChangedPundle changedPundle = new ChangedPundle(pundle.attributeValue("action"),
                        pundleType, pundle.attributeValue("name"),
                        pundle.attributeValue("version"));

                if (blessings.isEmpty()) {
                    newDeletion(changedPundle);
                    continue;
                }

                for (Object eachBlessing : blessings) {
                    Element blessing = (Element) eachBlessing;
                    newEntry(blessing.attributeValue("user"),
                            blessing.attributeValue("timestamp"),
                            blessing.getText().trim(), changedPundle);
                }
            }
        } catch (DocumentException e) {
            throw new IOException2("Failed to parse changelog file: ", e);
        }
        return new StoreChangeLogSet(build, entries);
    }

    private void newDeletion(ChangedPundle pundle) {
        newEntry("", null, "Pundles no longer used", pundle);
    }

    private void newEntry(String committer, String timestamp, String blessingComment, ChangedPundle pundle) {
        StoreChangeLogEntry entry = blessingMap.get(blessingComment);
        if (entry == null) {
            entry = new StoreChangeLogEntry(committer, timestamp, blessingComment);
            entries.add(entry);
            blessingMap.put(blessingComment, entry);
        }
        entry.updateTimestamp(timestamp);
        entry.addPundle(pundle);
    }
}
