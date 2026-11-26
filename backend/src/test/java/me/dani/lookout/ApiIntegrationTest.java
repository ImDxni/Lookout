package me.dani.lookout;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "lookout.admin.email=" + ApiIntegrationTest.ADMIN_EMAIL,
                "lookout.admin.password=" + ApiIntegrationTest.ADMIN_PASSWORD
        })
public abstract class ApiIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "test-password-123";

    @Autowired
    private Environment environment;

    protected ApiClient client;

    @BeforeEach
    void newClient() {
        client = new ApiClient(environment.getRequiredProperty("local.server.port", Integer.class));
    }

    protected ApiClient loggedInClient() {
        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);
        return client;
    }
}
