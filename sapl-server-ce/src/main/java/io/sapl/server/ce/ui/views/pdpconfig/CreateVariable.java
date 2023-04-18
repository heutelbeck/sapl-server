package io.sapl.server.ce.ui.views.pdpconfig;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import lombok.Setter;

public class CreateVariable extends VerticalLayout {
	private TextField nameTextField = new TextField("Variable Name");
	private Button    createButton  = new Button("Create");
	private Button    cancelButton  = new Button("Cancel");

	@Setter
	private UserConfirmedListener userConfirmedListener;

	public CreateVariable() {
		initUi();
	}

	public String getNameOfVariableToCreate() {
		return nameTextField.getValue();
	}

	private void initUi() {
		nameTextField.setPlaceholder("name");
		createButton.addClickListener(e -> setConfirmationResult(true));
		cancelButton.addClickListener(e -> setConfirmationResult(false));
		nameTextField.focus();

		var buttonLayout = new HorizontalLayout(cancelButton, createButton);
		add(nameTextField, buttonLayout);
	}

	private void setConfirmationResult(boolean isConfirmed) {
		if (userConfirmedListener != null) {
			userConfirmedListener.onConfirmationSet(isConfirmed);
		}
	}

	public interface UserConfirmedListener {
		void onConfirmationSet(boolean isConfirmed);
	}
}
