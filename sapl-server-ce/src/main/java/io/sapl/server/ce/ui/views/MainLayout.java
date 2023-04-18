package io.sapl.server.ce.ui.views;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.sapl.server.ce.security.AuthenticatedUser;
import io.sapl.server.ce.ui.components.appnav.AppNav;
import io.sapl.server.ce.ui.components.appnav.AppNavItem;
import io.sapl.server.ce.ui.views.clientcredentials.ClientCredentialsView;
import io.sapl.server.ce.ui.views.digitalpolicies.DigitalPoliciesView;
import io.sapl.server.ce.ui.views.digitalpolicies.PublishedPoliciesView;
import io.sapl.server.ce.ui.views.librariesdocumentation.LibrariesDocumentationView;
import io.sapl.server.ce.ui.views.pdpconfig.PDPConfigView;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

	private H2 viewTitle;

	private AuthenticatedUser       authenticatedUser;
	private AccessAnnotationChecker accessChecker;

	public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker) {
		this.authenticatedUser = authenticatedUser;
		this.accessChecker     = accessChecker;

		setPrimarySection(Section.DRAWER);
		addDrawerContent();
		addHeaderContent();
	}

	private void addHeaderContent() {
		DrawerToggle toggle = new DrawerToggle();
		toggle.getElement().setAttribute("aria-label", "Menu toggle");

		viewTitle = new H2();
		viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

		addToNavbar(true, toggle, viewTitle);
	}

	private void addDrawerContent() {
		H1 appName = new H1("SAPL Server CE");
		appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
		Header header = new Header(appName);

		Scroller scroller = new Scroller(createNavigation());

		addToDrawer(header, scroller, createFooter());
	}

	private AppNav createNavigation() {
		// AppNav is not yet an official component.
		// For documentation, visit https://github.com/vaadin/vcf-nav#readme
		AppNav nav = new AppNav();

		if (accessChecker.hasAccess(DigitalPoliciesView.class)) {
			nav.addItem(
					new AppNavItem("Digital Policies", DigitalPoliciesView.class, LineAwesomeIcon.FILE_SOLID.create()));

		}
		if (accessChecker.hasAccess(PublishedPoliciesView.class)) {
			nav.addItem(new AppNavItem("Published Policies", PublishedPoliciesView.class,
					LineAwesomeIcon.FILE_ALT.create()));

		}
		if (accessChecker.hasAccess(PDPConfigView.class)) {
			nav.addItem(new AppNavItem("PDP Config", PDPConfigView.class, LineAwesomeIcon.COG_SOLID.create()));

		}
		if (accessChecker.hasAccess(LibrariesDocumentationView.class)) {
			nav.addItem(new AppNavItem("Libraries Documentation", LibrariesDocumentationView.class,
					LineAwesomeIcon.BOOK_SOLID.create()));

		}
		if (accessChecker.hasAccess(ClientCredentialsView.class)) {
			nav.addItem(new AppNavItem("Client Credentials", ClientCredentialsView.class,
					LineAwesomeIcon.KEY_SOLID.create()));
		}

		return nav;
	}

	private Footer createFooter() {
		Footer layout = new Footer();

		Optional<UserDetails> maybeUser = authenticatedUser.get();
		if (maybeUser.isPresent()) {
			var user = maybeUser.get();

			var avatar = new Avatar(user.getUsername());
//            var resource = new StreamResource("profile-pic",
//                    () -> new ByteArrayInputStream(user.getProfilePicture()));
//			avatar.setImageResource(resource);
			avatar.setThemeName("xsmall");
			avatar.getElement().setAttribute("tabindex", "-1");

			var userMenu = new MenuBar();
			userMenu.setThemeName("tertiary-inline contrast");

			var userName = userMenu.addItem("");
			var div      = new Div();
			div.add(avatar);
			div.add(user.getUsername());
			div.add(new Icon("lumo", "dropdown"));
			div.getElement().getStyle().set("display", "flex");
			div.getElement().getStyle().set("align-items", "center");
			div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
			userName.add(div);
			userName.getSubMenu().addItem("Sign out", e -> authenticatedUser.logout());

			layout.add(userMenu);
		} else {
			var loginLink = new Anchor("login", "Sign in");
			layout.add(loginLink);
		}

		return layout;
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		viewTitle.setText(getCurrentPageTitle());
	}

	private String getCurrentPageTitle() {
		PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
		return title == null ? "" : title.value();
	}
}
