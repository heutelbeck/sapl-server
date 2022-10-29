/*
 * Copyright © 2017-2021 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.vaadin;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class BaseEditor extends Component {

	private static final String IsReadOnlyKey = "isReadOnly";
	
    private String document;
    private final List<DocumentChangedListener> documentChangedListeners;

    public BaseEditor() {
        this.documentChangedListeners = new ArrayList<>();
    }
    
    protected static void applyBaseConfiguration(Element element, BaseEditorConfiguration config) {
    	element.setProperty("hasLineNumbers", config.isHasLineNumbers());
		element.setProperty("autoCloseBrackets", config.isAutoCloseBrackets());
		element.setProperty("matchBrackets", config.isMatchBrackets());
		element.setProperty("textUpdateDelay", config.getTextUpdateDelay());
		element.setProperty(IsReadOnlyKey, config.isReadOnly());
		element.setProperty("isLint", config.isLint());
		element.setProperty("isDarkTheme", config.isDarkTheme());
    }

    @ClientCallable
	protected void onDocumentChanged(String newValue) {
		document = newValue;
		for (DocumentChangedListener listener : documentChangedListeners) {
			listener.onDocumentChanged(new DocumentChangedEvent(newValue));
		}
	}
    
    /**
	 * Sets the current document for the editor.
	 * 
	 * @param document The current document.
	 */
	public void setDocument(String document) {
		this.document = document;
		Element element = getElement();
		element.callJsFunction("setEditorDocument", element, document);
	}

	/**
	 * Returns the current document from the editor.
	 * 
	 * @return The current document from the editor.
	 */
	public String getDocument() {
		return document;
	}

	/**
	 * Registers a document changed listener. The document changed event will be
	 * raised when the document was changed in the editor.
	 * 
	 * @param listener The listener that will be called upon event invocation.
	 */
	public void addDocumentChangedListener(DocumentChangedListener listener) {
		this.documentChangedListeners.add(listener);
	}

	/**
	 * Removes a registered document changed listener.
	 * 
	 * @param listener The registered listener that should be removed.
	 */
	public void removeDocumentChangedListener(DocumentChangedListener listener) {
		this.documentChangedListeners.remove(listener);
	}

	/**
	 * This function enables or disables the read-only mode of the editor.
	 *
	 * @param isReadOnly
	 */
	public void setReadOnly(Boolean isReadOnly) {
		Element element = getElement();
		element.setProperty(IsReadOnlyKey, isReadOnly);
	}

	/**
	 * This function returns the current read-only status of the editor.
	 *
	 * @return The current read-only as a Boolean.
	 */
	public Boolean isReadOnly() {
		Element element = getElement();
		return element.getProperty(IsReadOnlyKey, false);
	}

	/**
	 * If this function is called the editor scrolls to the bottom of the textarea.
	 */
	public void scrollToBottom() {
		Element element = getElement();
		element.callJsFunction("scrollToBottom");
	}

	/**
	 * This function enables or disables the Dark Theme of the editor.
	 *
	 * @param isDarkTheme
	 */
	public void setDarkTheme(Boolean isDarkTheme) {
		Element element = getElement();
		element.setProperty("isDarkTheme", isDarkTheme);
	}

	/**
	 * This function returns a Boolean to whether the editor uses the Dark Theme or not.
	 *
	 * @return Enabled or disabled Dark Theme as a Boolean.
	 */
	public Boolean isDarkTheme() {
		Element element = getElement();
		return element.getProperty("isDarkTheme", false);
	}

	/**
	 * This function returns a Boolean whether if the editor has linting enabled or not.
	 *
	 * @return Enabled or disabled lint as a Boolean.
	 */
	public Boolean isLint() {
		Element element = getElement();
		return element.getProperty("isLint", true);
	}
}