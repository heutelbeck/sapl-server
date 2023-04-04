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
package io.sapl.server.ce.ui.views.librariesdocumentation;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.sapl.interpreter.functions.LibraryDocumentation;
import io.sapl.interpreter.pip.PolicyInformationPointDocumentation;
import io.sapl.server.ce.ui.views.MainLayout;
import io.sapl.spring.pdp.embedded.FunctionLibrariesDocumentation;
import io.sapl.spring.pdp.embedded.PolicyInformationPointsDocumentation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RolesAllowed("ADMIN")
@RequiredArgsConstructor
@PageTitle("Libraries Documentation")
@Route(value = LibrariesDocumentationView.ROUTE, layout = MainLayout.class)
public class LibrariesDocumentationView extends VerticalLayout {
	public static final String ROUTE = "libraries";

	private final FunctionLibrariesDocumentation       functionLibrariesDocumentation;
	private final PolicyInformationPointsDocumentation policyInformationPointsDocumentation;

	private Grid<LibraryDocumentation>                functionLibsGrid                   = new Grid<>();
	private VerticalLayout                            showCurrentFunctionLibLayout       = new VerticalLayout();
	private Div                                       descriptionOfCurrentFunctionLibDiv = new Div();;
	private Grid<Entry<String, String>>               functionsOfCurrentFunctionLibGrid  = new Grid<>();
	private Grid<PolicyInformationPointDocumentation> pipsGrid                           = new Grid<>();
	private VerticalLayout                            showCurrentPipLayout               = new VerticalLayout();
	private Div                                       descriptionOfCurrentPipDiv         = new Div();
	private Grid<Entry<String, String>>               functionsOfCurrentPipGrid          = new Grid<>();

	@PostConstruct
	private void init() {
		add(new H1("Function Libraries"));
		functionLibsGrid.setWidthFull();
		showCurrentFunctionLibLayout.add(descriptionOfCurrentFunctionLibDiv, functionsOfCurrentFunctionLibGrid);
		var functionsLayout = new SplitLayout(functionLibsGrid, showCurrentFunctionLibLayout);
		functionsLayout.setWidthFull();
		add(functionsLayout);

		add(new H1("Policy Information Points"));
		pipsGrid.setWidthFull();
		showCurrentPipLayout.add(descriptionOfCurrentPipDiv, functionsOfCurrentPipGrid);
		var pipsLayout = new SplitLayout(pipsGrid, showCurrentPipLayout);
		pipsLayout.setWidthFull();
		add(pipsLayout);

		initUiForFunctions();
		initUiForPips();
	}

	private void initUiForFunctions() {
		showCurrentFunctionLibLayout.setVisible(false);

		Collection<LibraryDocumentation>                 availableFunctionLibs                 = functionLibrariesDocumentation
				.getDocumentation();
		CallbackDataProvider<LibraryDocumentation, Void> dataProviderForCurrentFunctionLibGrid = DataProvider
				.fromCallbacks(query -> {
																											int offset = query
																													.getOffset();
																											int limit = query
																													.getLimit();

																											return availableFunctionLibs
																													.stream()
																													.skip(offset)
																													.limit(limit);
																										},
						query -> availableFunctionLibs.size());

		functionLibsGrid.setSelectionMode(SelectionMode.SINGLE);
		functionLibsGrid.addColumn(LibraryDocumentation::getName)
				.setHeader("Name");
		functionLibsGrid.addColumn(LibraryDocumentation::getDescription)
				.setHeader("Description");
		functionsOfCurrentFunctionLibGrid.addColumn(Entry<String, String>::getKey)
				.setHeader("Function");
		functionsOfCurrentFunctionLibGrid.addColumn(Entry<String, String>::getValue)
				.setHeader("Documentation")
				.setFlexGrow(5);
		functionsOfCurrentFunctionLibGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		functionsOfCurrentFunctionLibGrid.setSelectionMode(SelectionMode.NONE);
		functionLibsGrid.addSelectionListener(selection -> {
			Optional<LibraryDocumentation> optionalSelectedFunctionLib = selection.getFirstSelectedItem();
			optionalSelectedFunctionLib.ifPresentOrElse((LibraryDocumentation selectedFunctionLib) -> {
				showCurrentFunctionLibLayout.setVisible(true);

				descriptionOfCurrentFunctionLibDiv.setText(selectedFunctionLib.getDescription());

				var                                               documentation                                    = selectedFunctionLib
						.getDocumentation();
				var                                               documentationAsEntrySet                          = documentation
						.entrySet();
				CallbackDataProvider<Entry<String, String>, Void> dataProviderForFunctionsOfCurrentFunctionLibGrid = DataProvider
						.fromCallbacks(query -> {
																																int offset = query
																																		.getOffset();
																																int limit = query
																																		.getLimit();

																																return documentationAsEntrySet
																																		.stream()
																																		.skip(offset)
																																		.limit(limit);
																															},
								query -> documentationAsEntrySet.size());

				functionsOfCurrentFunctionLibGrid.setItems(dataProviderForFunctionsOfCurrentFunctionLibGrid);
			}, () -> showCurrentFunctionLibLayout.setVisible(false));
		});
		functionLibsGrid.setItems(dataProviderForCurrentFunctionLibGrid);

		// preselect first function lib if available
		if (!availableFunctionLibs.isEmpty()) {
			functionLibsGrid.select(availableFunctionLibs.iterator().next());
		}
	}

	private void initUiForPips() {
		showCurrentPipLayout.setVisible(false);

		Collection<PolicyInformationPointDocumentation>                 availablePips                 = policyInformationPointsDocumentation
				.getDocumentation();
		CallbackDataProvider<PolicyInformationPointDocumentation, Void> dataProviderForCurrentPipGrid = DataProvider
				.fromCallbacks(query -> {
																													int offset = query
																															.getOffset();
																													int limit = query
																															.getLimit();

																													return availablePips
																															.stream()
																															.skip(offset)
																															.limit(limit);
																												},
						query -> availablePips.size());

		pipsGrid.setSelectionMode(SelectionMode.SINGLE);
		pipsGrid.addColumn(PolicyInformationPointDocumentation::getName)
				.setHeader("Name");
		pipsGrid.addColumn(PolicyInformationPointDocumentation::getDescription)
				.setHeader("Description");
		functionsOfCurrentPipGrid.addColumn(Entry<String, String>::getKey)
				.setHeader("Function");
		functionsOfCurrentPipGrid.addColumn(Entry<String, String>::getValue)
				.setHeader("Documentation")
				.setFlexGrow(5);
		functionsOfCurrentPipGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
		functionsOfCurrentPipGrid.setSelectionMode(SelectionMode.NONE);
		pipsGrid.addSelectionListener(selection -> {
			Optional<PolicyInformationPointDocumentation> optionalSelectedPip = selection.getFirstSelectedItem();
			optionalSelectedPip.ifPresentOrElse((PolicyInformationPointDocumentation selectedPip) -> {
				showCurrentPipLayout.setVisible(true);

				descriptionOfCurrentPipDiv.setText(selectedPip.getDescription());

				Map<String, String>                               documentation                            = selectedPip
						.getDocumentation();
				Set<Entry<String, String>>                        documentationAsEntrySet                  = documentation
						.entrySet();
				CallbackDataProvider<Entry<String, String>, Void> dataProviderForFunctionsOfCurrentPipGrid = DataProvider
						.fromCallbacks(query -> {
																														int offset = query
																																.getOffset();
																														int limit = query
																																.getLimit();

																														return documentationAsEntrySet
																																.stream()
																																.skip(offset)
																																.limit(limit);
																													},
								query -> documentationAsEntrySet.size());
				functionsOfCurrentPipGrid.setItems(dataProviderForFunctionsOfCurrentPipGrid);
			}, () -> showCurrentPipLayout.setVisible(false));
		});
		pipsGrid.setItems(dataProviderForCurrentPipGrid);

		// preselect first PIP if available
		if (!availablePips.isEmpty()) {
			pipsGrid.select(availablePips.iterator().next());
		}
	}

}
