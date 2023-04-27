package io.sapl.server.ce.ui.views.login;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.sapl.server.ce.security.AuthenticatedUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@AnonymousAllowed
@PageTitle("Login")
@Route(value = "login")
@RequiredArgsConstructor
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

	private final transient AuthenticatedUser authenticatedUser;

	@PostConstruct
	void init() {
		setAction(RouteUtil.getRoutePath(VaadinService.getCurrent().getContext(), getClass()));

		var i18n = LoginI18n.createDefault();
		i18n.setHeader(new LoginI18n.Header());
		i18n.getHeader().setTitle("SAPL Server CE");
		i18n.getHeader().setDescription("Login with administrator account.");
		i18n.setAdditionalInformation(null);
		setI18n(i18n);

		setForgotPasswordButtonVisible(false);
		setOpened(true);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (authenticatedUser.get().isPresent()) {
			// Already logged in
			setOpened(false);
			event.forwardTo("");
		}

		setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
	}
}
