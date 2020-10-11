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

import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

/**
 * Represents a StorePundle that has been changed since the previous build.
 * <p>
 * Adapts pundles to Jenkins' AffectedFile API.
 *
 * @author Randy Coulman
 */
public class ChangedPundle implements ChangeLogSet.AffectedFile {
    private EditType editType;
    private PundleType pundleType;
    private String name;
    private String version;

    public ChangedPundle(String action, PundleType pundleType, String name, String version) {
        this.editType = toEditType(action);
        this.pundleType = pundleType;
        this.name = name;
        this.version = version;
    }

    public ChangedPundle(String action, PundleType pundleType, String name) {
        this(action, pundleType, name, null);
    }

    public String getPath() {
        return getDescriptor();
    }

    public EditType getEditType() {
        return editType;
    }

    public String getDescriptor() {
        String descriptor = pundleType.getDescription() + " " + name;
        if (isDeletion())
            return descriptor;

        return descriptor + " (" + version + ")";
    }

    private EditType toEditType(String action) {
        if (action.equals("added"))
            return EditType.ADD;
        if (action.equals("deleted"))
            return EditType.DELETE;

        return EditType.EDIT;
    }

    private boolean isDeletion() {
        return editType == EditType.DELETE;
    }
}
