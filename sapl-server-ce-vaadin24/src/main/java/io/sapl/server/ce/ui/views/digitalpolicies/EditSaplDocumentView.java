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
package io.sapl.server.ce.ui.views.digitalpolicies;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.sapl.server.ce.model.sapldocument.PublishedDocumentNameCollisionException;
import io.sapl.server.ce.model.sapldocument.SaplDocument;
import io.sapl.server.ce.model.sapldocument.SaplDocumentService;
import io.sapl.server.ce.model.sapldocument.SaplDocumentVersion;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import io.sapl.server.ce.ui.views.MainLayout;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View to edit a {@link SaplDocument}.
 */
@Slf4j
@RolesAllowed("ADMIN")
@RequiredArgsConstructor
@PageTitle("Edit SAPL document")
@Route(value = EditSaplDocumentView.ROUTE, layout = MainLayout.class)
public class EditSaplDocumentView extends VerticalLayout
		implements HasUrlParameter<Long> {
	public static final String  ROUTE                       = "documents";
	private static final String NEW_VERSION_SELECTION_ENTRY = "New Version";

	private final transient SaplDocumentService saplDocumentService;

	private transient SaplDocumentVersion selectedSaplDocumentVersion;
	private boolean                       isSelectedVersionRestoredViaEditedDocument;

	private TextField policyIdField         = new TextField("Policy Identifier");
	private TextField currentVersionField   = new TextField("Current Version");
	private TextField lastModifiedField     = new TextField("Last Modified");
	private TextField publishedVersionField = new TextField("Published Version");
	private TextField publishedNameField    = new TextField("Published Name");
// TODO:	private SaplEditor       saplEditor;
	private TextArea         saplEditor        = new TextArea();
	private ComboBox<String> versionSelection  = new ComboBox<>("Version History");
	private Button           saveVersionButton = new Button("Save New Version");
	private Button           cancelButton      = new Button("Cancel");
	private Button           publishButton     = new Button("Publish Selected Version");
	private Button           unpublishButton   = new Button("Unpublish");

	private SaplDocument saplDocument;
	private long         saplDocumentId;
	private boolean      isFirstDocumentValueValidation;

	@PostConstruct
	private void init() {
		var metadateRowOne   = new HorizontalLayout(policyIdField, currentVersionField, lastModifiedField);
		var metadateRowTwo   = new HorizontalLayout(publishedVersionField, publishedNameField);
		var metadateRowThree = new HorizontalLayout(versionSelection, publishButton, unpublishButton);
		var editActionsRow   = new HorizontalLayout(saveVersionButton, cancelButton);
		editActionsRow.setWidthFull();
		editActionsRow.setJustifyContentMode(JustifyContentMode.END);
		saplEditor.setWidthFull();
		add(metadateRowOne, metadateRowTwo, metadateRowThree, saplEditor, editActionsRow);
	}

	@Override
	public void setParameter(BeforeEvent event, Long parameter) {
		saplDocumentId = parameter;

		reloadSaplDocument();
		addListener();

		addAttachListener((__) -> {
			if (saplDocument == null) {
				log.warn("SAPL document with id {} is not existing, redirect to list view", parameter);
				getUI().ifPresent(ui -> ui.navigate(DigitalPoliciesView.ROUTE));
			}
		});
	}

	private void reloadSaplDocument() {
		Optional<SaplDocument> optionalSaplDocument = saplDocumentService.getById(saplDocumentId);
		if (optionalSaplDocument.isEmpty()) {
			// Vaadin UI object is not available yet, redirect to list view via attach
			// listener
			return;
		}

		saplDocument = optionalSaplDocument.get();
		setUI();

		saveVersionButton.setEnabled(false);

		isFirstDocumentValueValidation = true;
	}

	private void addListener() {
		versionSelection.addValueChangeListener(changedEvent -> {
			updateSaplEditorBasedOnVersionSelection();
		});
//		saplEditor.addValidationFinishedListener(validationFinishedEvent -> {
		saplEditor.addValidationStatusChangeListener(validationFinishedEvent -> {
			if (isFirstDocumentValueValidation) {
				isFirstDocumentValueValidation = false;
				return;
			}

			var document = saplEditor.getValue();

			if (selectedSaplDocumentVersion != null
					&& selectedSaplDocumentVersion.getDocumentContent().equals(document)) {
				isSelectedVersionRestoredViaEditedDocument = true;
				versionSelection.setValue(Integer.toString(selectedSaplDocumentVersion.getVersionNumber()));
				return;
			}

			versionSelection.setValue(NEW_VERSION_SELECTION_ENTRY);

//			Issue[] issues               = validationFinishedEvent.getIssues();
//			boolean areNoIssuesAvailable = issues.length == 0
//					&& document.length() <= SaplDocumentVersion.MAX_DOCUMENT_SIZE;
			var areNoIssuesAvailable = validationFinishedEvent.getNewStatus();
			saveVersionButton.setEnabled(areNoIssuesAvailable);
		});

		saveVersionButton.addClickListener(clickEvent -> {
			saplDocumentService.createVersion(saplDocumentId, saplEditor.getValue());
			reloadSaplDocument();
		});

		cancelButton.addClickListener(clickEvent -> {
			cancelButton.getUI().ifPresent(ui -> ui.navigate(DigitalPoliciesView.ROUTE));
		});

		publishButton.addClickListener(clickEvent -> {
			Optional<Integer> selectedVersionNumberAsOptional = getSelectedVersionNumber();
			if (selectedVersionNumberAsOptional.isEmpty()) {
				return;
			}

			try {
				saplDocumentService.publishPolicyVersion(saplDocumentId, selectedVersionNumberAsOptional.get());
			} catch (PublishedDocumentNameCollisionException ex) {
				ErrorNotificationUtils.show(ex.getMessage());
				return;
			}

			reloadSaplDocument();
		});

		unpublishButton.addClickListener(clickEvent -> {
			saplDocumentService.unpublishPolicy(saplDocumentId);
			reloadSaplDocument();
		});
	}

	/**
	 * Imports the previously set instance of {@link SaplDocument} to the UI.
	 */
	private void setUI() {
		policyIdField.setValue(saplDocument.getId().toString());
		lastModifiedField.setValue(saplDocument.getLastModified());
		currentVersionField.setValue(Integer.toString(saplDocument.getCurrentVersionNumber()));

		Collection<String> availableVersions = getAvailableVersions();
		versionSelection.setItems(availableVersions);
		versionSelection.setValue(Iterables.getLast(availableVersions));

		SaplDocumentVersion currentVersion = saplDocument.getCurrentVersion();
		saplEditor.setValue(currentVersion.getDocumentContent());
		selectedSaplDocumentVersion = currentVersion;

		setUiForPublishing();
	}

	private void setUiForPublishing() {
		SaplDocumentVersion publishedVersion           = saplDocument.getPublishedVersion();
		boolean             isPublishedVersionExisting = publishedVersion != null;

		String publishedVersionAsString;
		String publishedNameAsString;

		if (isPublishedVersionExisting) {
			String publishedName = publishedVersion.getName();

			publishedVersionAsString = Integer.toString(publishedVersion.getVersionNumber());
			publishedNameAsString    = publishedName;

			Optional<Integer> selectedVersionNumber = getSelectedVersionNumber();
			if (selectedVersionNumber.isPresent()) {
				SaplDocumentVersion selectedVersion = saplDocument.getVersion(selectedVersionNumber.get());

				boolean isSelectedVersionPublished = publishedVersion.getVersionNumber() == selectedVersion
						.getVersionNumber();
				publishButton.setEnabled(!isSelectedVersionPublished);
			}
		} else {
			publishButton.setEnabled(true);

			publishedVersionAsString = "-";
			publishedNameAsString    = "-";
		}

		publishedVersionField.setValue(publishedVersionAsString);
		publishedNameField.setValue(publishedNameAsString);

		unpublishButton.setEnabled(isPublishedVersionExisting);
	}

	private Collection<String> getAvailableVersions() {
		// @formatter:off
		return saplDocument.getVersions().stream()
				.map(saplDocumentVersion -> Integer.toString(saplDocumentVersion.getVersionNumber()))
				.collect(Collectors.toList());
		// @formatter:on
	}

	private void updateSaplEditorBasedOnVersionSelection() {
		Optional<Integer> selectedVersionNumberAsOptional = getSelectedVersionNumber();
		if (!selectedVersionNumberAsOptional.isPresent()) {
			publishButton.setEnabled(false);
			unpublishButton.setEnabled(false);
			return;
		}

		selectedSaplDocumentVersion = saplDocument.getVersion(selectedVersionNumberAsOptional.get());

		if (isSelectedVersionRestoredViaEditedDocument) {
			/*
			 * do not update content of sapl editor if change is initiated via restored
			 * document after editing (do not reset cursor position of editor)
			 */
			isSelectedVersionRestoredViaEditedDocument = false;
		} else {
			saplEditor.setValue(selectedSaplDocumentVersion.getDocumentContent());
			isFirstDocumentValueValidation = true;
		}

		saveVersionButton.setEnabled(false);

		setUiForPublishing();
	}

	/**
	 * Gets the currently selected version number.
	 * 
	 * @return the version number as {@link Optional} (empty if no explicit version
	 *         is selected)
	 */
	private Optional<Integer> getSelectedVersionNumber() {
		String selectedVersionAsString = versionSelection.getValue();
		if (selectedVersionAsString == null || NEW_VERSION_SELECTION_ENTRY.equals(selectedVersionAsString)) {
			return Optional.empty();
		}

		return Optional.of(Integer.valueOf(selectedVersionAsString));
	}
}
