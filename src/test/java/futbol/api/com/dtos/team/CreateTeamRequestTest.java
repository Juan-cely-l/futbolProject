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

class CreateTeamRequestTest {

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
    @DisplayName("Should pass validation with valid fields")
    void validRequest() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Real Madrid");
        request.setBudget(500_000_000L);
        request.setCity("Madrid");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when name is blank")
    void blankName() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("");
        request.setBudget(100L);
        request.setCity("Bogota");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when name is null")
    void nullName() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName(null);
        request.setBudget(100L);
        request.setCity("Bogota");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when budget is null")
    void nullBudget() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Team");
        request.setBudget(null);
        request.setCity("City");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("budget"));
    }

    @Test
    @DisplayName("Should fail when city is blank")
    void blankCity() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Team");
        request.setBudget(100L);
        request.setCity("");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("city"));
    }

    @Test
    @DisplayName("Should fail with multiple violations when all fields are invalid")
    void allFieldsInvalid() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("");
        request.setBudget(null);
        request.setCity("");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(5);
    }
}
