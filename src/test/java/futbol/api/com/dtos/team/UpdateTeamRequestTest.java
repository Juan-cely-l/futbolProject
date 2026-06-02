package futbol.api.com.dtos.team;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateTeamRequestTest {

    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void afterAll() {
        if (factory != null) factory.close();
    }

    @Test
    @DisplayName("Should pass with all null fields (partial update)")
    void allNullFields() {
        UpdateTeamRequest request = new UpdateTeamRequest();

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass with valid name")
    void validName() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Real Madrid");

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when name is too short")
    void nameTooShort() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("A");

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when city is too short")
    void cityTooShort() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setCity("X");

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("city"));
    }

    @Test
    @DisplayName("Should pass with valid budget")
    void validBudget() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setBudget(1_000_000L);

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass with all valid fields")
    void allFieldsValid() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("FC Barcelona");
        request.setBudget(300_000_000L);
        request.setCity("Barcelona");

        Set<ConstraintViolation<UpdateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
