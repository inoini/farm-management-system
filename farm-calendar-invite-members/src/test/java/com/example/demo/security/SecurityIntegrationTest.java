package com.example.demo.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.RegistrationService;

@SpringBootTest(classes = SecurityIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, LoginController.class, ProtectedEndpoint.class})
    static class TestApplication {}

    @RestController
    static class ProtectedEndpoint {
        @GetMapping("/") String home() { return "private"; }
    }

    @MockBean UserAccountRepository userRepository;
    @MockBean RegistrationService registrationService;
    @Autowired MockMvc mvc;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginRegisterAndHealthArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/health")).andExpect(status().isOk());
    }
}
