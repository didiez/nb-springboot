/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.alexfalappa.nbspringboot.cfgprops.fixes;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.EnhancedFix;
import org.openide.awt.StatusDisplayer;

/**
 * {@link EnhancedFix} implementation to replace a deprecated config property with its replacement.
 *
 * @author Alessandro Falappa
 */
public class ReplacePropFix implements BaseFix {

    private final Document document;
    private final int start;
    private final int end;
    private final String bodyText;
    private final String replacement;

    public ReplacePropFix(Document document, int start, int end, String replacement) throws BadLocationException {
        this.document = document;
        this.start = start;
        this.end = end;
        this.bodyText = document.getText(start, end - start);
        this.replacement = replacement;
    }

    @Override
    public String getText() {
        return String.format("Use replacement '%s'", replacement);
    }

    @Override
    public CharSequence getSortText() {
        return SORT_REPLACEMENT;
    }

    @Override
    public ChangeInfo implement() throws Exception {
        document.remove(start, end - start);
        document.insertString(start, replacement, null);
        StatusDisplayer.getDefault().setStatusText("Replaced property: " + bodyText);
        return null;
    }

}
