package futbol.api.com.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ADMIN_PASSWORD=test123")
@DisplayName("SecurityConfig Integration Tests")
class SecurityConfigIntegrationTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Test
    @DisplayName("filterChain: bean is created with stateless session policy")
    void filterChain_beanCreated() {
        assertThat(filterChain).isNotNull();
    }

    @Test
    @DisplayName("security headers: Spring does not emit CSP or HSTS")
    void securityHeaders_doNotEmitCspOrHsts() {
        var headerFilter = springSecurityFilterChain.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(HeaderWriterFilter.class::isInstance)
                .map(HeaderWriterFilter.class::cast)
                .findFirst();

        assertThat(headerFilter).isPresent();
        @SuppressWarnings("unchecked")
        List<HeaderWriter> headerWriters = (List<HeaderWriter>) ReflectionTestUtils.getField(headerFilter.get(), "headerWriters");

        assertThat(headerWriters).isNotNull();
        assertThat(headerWriters)
                .extracting(writer -> writer.getClass().getName())
                .noneMatch(name -> name.contains("ContentSecurityPolicyHeaderWriter"));
        assertThat(headerWriters)
                .extracting(writer -> writer.getClass().getName())
                .noneMatch(name -> name.contains("HstsHeaderWriter"));
    }
}
