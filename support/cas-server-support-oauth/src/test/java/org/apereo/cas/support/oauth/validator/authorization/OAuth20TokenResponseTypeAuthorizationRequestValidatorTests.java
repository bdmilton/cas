package org.apereo.cas.support.oauth.validator.authorization;

import org.apereo.cas.AbstractOAuth20Tests;
import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.services.RegisteredServiceAccessStrategyAuditableEnforcer;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.jee.context.JEEContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link OAuth20TokenResponseTypeAuthorizationRequestValidatorTests}.
 *
 * @author Julien Huon
 * @since 6.4.0
 */
@Tag("OAuth")
public class OAuth20TokenResponseTypeAuthorizationRequestValidatorTests extends AbstractOAuth20Tests {
    @Test
    public void verifySupports() throws Exception {
        val service = new OAuthRegisteredService();
        service.setName("OAuth");
        service.setClientId(UUID.randomUUID().toString());
        service.setClientSecret("secret");
        service.setServiceId("https://callback.example.org");
        servicesManager.save(service);

        val validator = new OAuth20TokenResponseTypeAuthorizationRequestValidator(servicesManager, new WebApplicationServiceFactory(),
            new RegisteredServiceAccessStrategyAuditableEnforcer(applicationContext), oauthRequestParameterResolver);

        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);

        request.setParameter(OAuth20Constants.RESPONSE_TYPE, OAuth20ResponseTypes.CODE.getType());
        request.setParameter(OAuth20Constants.CLIENT_ID, service.getClientId());
        request.setParameter(OAuth20Constants.REDIRECT_URI, service.getServiceId());
        assertFalse(validator.supports(context));

        request.setParameter(OAuth20Constants.RESPONSE_TYPE, OAuth20ResponseTypes.TOKEN.getType());
        assertTrue(validator.supports(context));
    }
}
