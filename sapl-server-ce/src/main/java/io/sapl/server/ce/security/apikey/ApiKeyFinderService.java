package io.sapl.server.ce.security.apikey;

import io.sapl.server.ce.model.clients.ClientCredentials;
import io.sapl.server.ce.model.clients.ClientCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyFinderService {
	private final PasswordEncoder passwordEncoder;
	private final ClientCredentialsRepository clientCredentialsRepository;

	public boolean isApiKeyAssociatedWithClientCredentials(String apiKey){
		for (ClientCredentials c : clientCredentialsRepository.getApiKeyCredentials()) {
			if (passwordEncoder.matches(apiKey, c.getEncodedSecret())){
				return true;
			}
		}
		return false;
	}
}
