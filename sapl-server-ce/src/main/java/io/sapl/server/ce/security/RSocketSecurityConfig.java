package io.sapl.server.ce.security;

import io.sapl.server.ce.security.apikey.ApiKeyPayloadExchangeAuthenticationConverterService;
import io.sapl.server.ce.security.apikey.ApiKeyReactiveAuthenticationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.PayloadInterceptorOrder;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.rsocket.authentication.AuthenticationPayloadExchangeConverter;
import org.springframework.security.rsocket.authentication.AuthenticationPayloadInterceptor;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@EnableRSocketSecurity
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class RSocketSecurityConfig {

    @Value("${io.sapl.server.accesscontrol.allowBasicAuth:#{true}}")
    private boolean  allowBasicAuth;
    @Value("${io.sapl.server.accesscontrol.allowApiKeyAuth:#{true}}")
    private boolean  allowApiKeyAuth;

    @Value("${io.sapl.server.accesscontrol.allowApiKeyAuth:#{false}}")
    private boolean allowOauth2Auth;

    @Value("spring.security.oauth2.resourceserver.jwt.issuer-uri:null")
    private String jwtIssuerURI;

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final ApiKeyPayloadExchangeAuthenticationConverterService apiKeyPayloadExchangeAuthenticationConverterService;


    ReactiveUserDetailsService rsocketUserDetailsService = new ReactiveUserDetailsService(){
        @Override
        public Mono<UserDetails> findByUsername(String username) {
            return Mono.just(userDetailsService.loadUserByUsername(username));
        }
    };

    private static void customize(RSocketSecurity.AuthorizePayloadsSpec spec) {
        spec.anyRequest().authenticated().anyExchange().permitAll();
    }


    /**
     * The PayloadSocketAcceptorInterceptor Bean (rsocketPayloadAuthorization)
     * configures the Security Filter Chain for Rsocket Payloads. Supported
     * Authentication Methods are: NoAuth, BasicAuth, Oauth2 (jwt) and ApiKey.
     */
    @Bean
    PayloadSocketAcceptorInterceptor rsocketPayloadAuthorization(RSocketSecurity security) {
        security = security.authorizePayload(RSocketSecurityConfig::customize);

        // Configure Basic Authentication
        UserDetailsRepositoryReactiveAuthenticationManager simpleManager = null;
        if (allowBasicAuth) {
            log.info("configuring BasicAuth for RSocket authentication");
            simpleManager = new  UserDetailsRepositoryReactiveAuthenticationManager(rsocketUserDetailsService);
            simpleManager.setPasswordEncoder(passwordEncoder);
        }

        // Configure Oauth2 Authentication
        JwtReactiveAuthenticationManager jwtManager = null;
        if (allowOauth2Auth) {
            jwtManager = new JwtReactiveAuthenticationManager(
                    ReactiveJwtDecoders.fromIssuerLocation(jwtIssuerURI));
        }

        UserDetailsRepositoryReactiveAuthenticationManager finalSimpleManager = simpleManager;
        JwtReactiveAuthenticationManager finalJwtManager = jwtManager;
        AuthenticationPayloadInterceptor auth               = new AuthenticationPayloadInterceptor(
                a -> {
                    if (finalSimpleManager != null
                            && a instanceof UsernamePasswordAuthenticationToken) {
                        return finalSimpleManager.authenticate(a);
                    } else if (finalJwtManager != null
                            && a instanceof BearerTokenAuthenticationToken) {
                        return finalJwtManager
                                .authenticate(
                                        a);
                    } else {
                        throw new IllegalArgumentException(
                                "Unsupported Authentication Type "
                                        + a.getClass()
                                        .getSimpleName());
                    }
                });
        auth.setAuthenticationConverter(new AuthenticationPayloadExchangeConverter());
        auth.setOrder(PayloadInterceptorOrder.AUTHENTICATION.getOrder());
        security.addPayloadInterceptor(auth);

        // Configure ApiKey authentication
        if (allowApiKeyAuth) {
            log.info("configuring ApiKey for RSocket authentication");
            ReactiveAuthenticationManager manager           = new ApiKeyReactiveAuthenticationManager();
            AuthenticationPayloadInterceptor apikeyInterceptor = new AuthenticationPayloadInterceptor(manager);
            apikeyInterceptor
                    .setAuthenticationConverter(apiKeyPayloadExchangeAuthenticationConverterService);
            apikeyInterceptor.setOrder(PayloadInterceptorOrder.AUTHENTICATION.getOrder());
            security.addPayloadInterceptor(apikeyInterceptor);
        }

        return security.build();

    }
}
