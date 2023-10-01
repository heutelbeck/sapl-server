package io.sapl.server.ce.security.apikey;

import io.sapl.server.ce.model.clients.ClientCredentialsRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeaderHeaderAuthFilterService extends GenericFilterBean {
    private final ClientCredentialsRepository clientCredentialsRepository;

    @Value("${io.sapl.server.accesscontrol.apiKeyHeaderName:API_KEY}")
    private String	apiKeyHeaderName;

    private boolean isApiKeyAssociatedWithClientCredentials(String apiKey){
        return clientCredentialsRepository.findByApiKey(apiKey).isPresent();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        var request = (HttpServletRequest) servletRequest;
        var response = (HttpServletResponse) servletResponse;
        // if header token is not valid, send un-athorized error back
        String apiKey = request.getHeader(apiKeyHeaderName);
        if (StringUtils.isNotEmpty(apiKey)) {
            if (isApiKeyAssociatedWithClientCredentials(apiKey)){
                Authentication auth = new ApiKeyAuthenticationToken(apiKey);
                auth.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
