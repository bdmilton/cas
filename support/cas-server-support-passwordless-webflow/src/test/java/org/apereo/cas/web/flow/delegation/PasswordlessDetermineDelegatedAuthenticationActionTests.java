package org.apereo.cas.web.flow.delegation;

import org.apereo.cas.api.PasswordlessUserAccount;
import org.apereo.cas.util.model.TriStateBoolean;
import org.apereo.cas.web.flow.BasePasswordlessAuthenticationActionTests;
import org.apereo.cas.web.flow.BaseWebflowConfigurerTests;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.PasswordlessWebflowUtils;

import lombok.val;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.util.Pac4jConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockRequestContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link PasswordlessDetermineDelegatedAuthenticationActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@Tag("WebflowAuthenticationActions")
public class PasswordlessDetermineDelegatedAuthenticationActionTests {
    @Nested
    @Import(BaseWebflowConfigurerTests.SharedTestConfiguration.class)
    @TestPropertySource(properties = {
        "cas.authn.passwordless.accounts.simple.casuser=casuser@example.org",
        "cas.authn.passwordless.core.delegated-authentication-activated=true",
        "cas.authn.passwordless.core.delegated-authentication-selector-script.location=classpath:/DelegatedAuthenticationSelectorScript.groovy"
    })
    @SuppressWarnings("ClassCanBeStatic")
    public class WithoutClients extends BasePasswordlessAuthenticationActionTests {
        @Autowired
        @Qualifier(CasWebflowConstants.ACTION_ID_DETERMINE_PASSWORDLESS_DELEGATED_AUTHN)
        private Action determineDelegatedAuthenticationAction;

        @Test
        public void verifyNoClients() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .username("casuser")
                .name("casuser")
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, determineDelegatedAuthenticationAction.execute(context).getId());
        }
    }

    @Nested
    @Import(BaseWebflowConfigurerTests.SharedTestConfiguration.class)
    @TestPropertySource(properties = {
        "cas.authn.pac4j.cas[0].login-url=https://casserver.herokuapp.com/cas/login",
        "cas.authn.pac4j.cas[0].protocol=CAS30",
        "cas.authn.passwordless.accounts.simple.casuser=casuser@example.org",
        "cas.authn.passwordless.core.delegated-authentication-activated=true",
        "cas.authn.passwordless.core.delegated-authentication-selector-script.location=classpath:/DelegatedAuthenticationSelectorScript.groovy"
    })
    @SuppressWarnings("ClassCanBeStatic")
    public class WithClients extends BasePasswordlessAuthenticationActionTests {
        @Autowired
        @Qualifier(CasWebflowConstants.ACTION_ID_DETERMINE_PASSWORDLESS_DELEGATED_AUTHN)
        private Action determineDelegatedAuthenticationAction;

        @Test
        public void verifyNoAcct() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_ERROR, determineDelegatedAuthenticationAction.execute(context).getId());
        }

        @Test
        public void verifyAction() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .username("casuser")
                .name("casuser")
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_PROMPT, determineDelegatedAuthenticationAction.execute(context).getId());
        }

        @Test
        public void verifyCantDetermineIdP() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .username("unknown")
                .name("unknown")
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, determineDelegatedAuthenticationAction.execute(context).getId());
        }

        @Test
        public void verifyActionByUser() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .delegatedAuthenticationEligible(TriStateBoolean.TRUE)
                .username("casuser")
                .name("casuser")
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_PROMPT, determineDelegatedAuthenticationAction.execute(context).getId());
            assertNotNull(request.getAttribute(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER));
        }

        @Test
        public void verifyActionByUserDisallowed() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .delegatedAuthenticationEligible(TriStateBoolean.TRUE)
                .allowedDelegatedClients(List.of("UnknownClient"))
                .username("casuser")
                .name("casuser")
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, determineDelegatedAuthenticationAction.execute(context).getId());
        }

        @Test
        public void verifyAuthInactive() throws Exception {
            val exec = new MockFlowExecutionContext(new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN)));
            val context = new MockRequestContext(exec);
            val request = new MockHttpServletRequest();
            val account = PasswordlessUserAccount.builder()
                .email("email")
                .phone("phone")
                .username("casuser")
                .name("casuser")
                .delegatedAuthenticationEligible(TriStateBoolean.FALSE)
                .build();
            PasswordlessWebflowUtils.putPasswordlessAuthenticationAccount(context, account);
            context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
            assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, determineDelegatedAuthenticationAction.execute(context).getId());
        }
    }
}
