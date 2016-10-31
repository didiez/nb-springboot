/*
 * Copyright 2015 Keevosh ULP.
 * Modifications copyright 2016 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgeditor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

import static com.github.alexfalappa.nbspringboot.cfgeditor.Utils.shortenJavaType;
import static com.github.alexfalappa.nbspringboot.cfgeditor.Utils.simpleHtmlEscape;

/**
 * The Spring Boot configuration property names implementation of {@code CompletionItem}.
 * <p>
 * It utilizes an {@code ItemMetadata} and the project classpath to render the completion item and spawn the documentation display.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
public class ConfigPropertyCompletionItem implements CompletionItem {

    private final ItemMetadata configurationItem;
    // may be null
    private final ItemHint hint;
    private final ClassPath classPath;
    private static final ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage(
            "com/github/alexfalappa/nbspringboot/cfgeditor/springboot-property.png"));
    private final int caretOffset;
    private final int propStartOffset;
    private boolean overwrite;

    public ConfigPropertyCompletionItem(ItemMetadata configurationItem, ItemHint hint, ClassPath classPath, int propStartOffset,
            int caretOffset) {
        this.overwrite = false;
        this.configurationItem = configurationItem;
        this.hint = hint;
        this.classPath = classPath;
        this.propStartOffset = propStartOffset;
        this.caretOffset = caretOffset;
    }

    public ItemMetadata getConfigurationItem() {
        return configurationItem;
    }

    public ItemHint getHint() {
        return hint;
    }

    public ClassPath getClassPath() {
        return classPath;
    }

    public String getText() {
        return configurationItem.getName();
    }

    public String getTextRight() {
        String type = configurationItem.getType();
        if (type == null) {
            return null;
        }
        return simpleHtmlEscape(shortenJavaType(type));
    }

    @Override
    public void defaultAction(JTextComponent jtc) {
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            // calculate the amount of chars to remove (by default from property start up to caret position)
            int lenToRemove = caretOffset - propStartOffset;
            if (overwrite) {
                // NOTE: the editor removes by itself the word at caret when ctrl + enter is pressed
                // the document state here is different from when the completion was invoked thus we have to
                // find again the offset of the equal sign in the line
                Element lineElement = doc.getParagraphElement(caretOffset);
                String line = doc.getText(lineElement.getStartOffset(), lineElement.getEndOffset() - lineElement.getStartOffset());
                int equalSignIndex = line.indexOf('=');
                if (equalSignIndex >= 0) {
                    // from property start to equal sign
                    lenToRemove = lineElement.getStartOffset() + equalSignIndex - propStartOffset;
                } else {
                    // from property start to end of line (except line terminator)
                    lenToRemove = lineElement.getEndOffset() - 1 - propStartOffset;
                }
            }
            // remove characters from the property name start offset
            doc.remove(propStartOffset, lenToRemove);
            doc.insertString(propStartOffset, getText(), null);
            // close the code completion box
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
        // detect if Ctrl + Enter is pressed
        overwrite = evt.getKeyCode() == KeyEvent.VK_ENTER && (evt.getModifiers() & KeyEvent.CTRL_MASK) != 0;
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(getText(), getTextRight(), graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width, int height, boolean selected) {
        String leftHtmlText = getText();
        if (configurationItem.getDeprecation() != null) {
            leftHtmlText = "<s>" + leftHtmlText + "</s>";
        }
        CompletionUtilities.renderHtml(fieldIcon, leftHtmlText, getTextRight(), g, defaultFont, (selected ? UIManager.getColor(
                "List.selectionForeground") : UIManager.getColor("List.foreground")), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                completionResultSet
                        .setDocumentation(new ConfigPropertyCompletionDocumentation(ConfigPropertyCompletionItem.this));
                completionResultSet.finish();
            }
        });
    }

    @Override
    public CompletionTask createToolTipTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                JToolTip toolTip = new JToolTip();
                toolTip.setTipText("Press Enter to insert \"" + getText() + "\"");
                completionResultSet.setToolTip(toolTip);
                completionResultSet.finish();
            }
        });
    }

    @Override
    public boolean instantSubstitution(JTextComponent component) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public CharSequence getSortText() {
        return getText();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return getText();
    }

}
