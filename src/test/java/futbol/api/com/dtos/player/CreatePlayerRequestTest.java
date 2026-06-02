package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
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

class CreatePlayerRequestTest {

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

    private CreatePlayerRequest createValidRequest() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName("Lionel Messi");
        request.setGoals(30);
        request.setPosition(Position.FORWARD);
        request.setAge(25);
        request.setAssists(15);
        request.setMatches(40);
        request.setValueMarket(100_000_000);
        request.setTeamName("FC Barcelona");
        return request;
    }

    @Test
    @DisplayName("Should pass validation with valid fields")
    void validRequestPasses() {
        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(createValidRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when name is blank")
    void blankName() {
        CreatePlayerRequest request = createValidRequest();
        request.setName("");

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when goals is negative")
    void negativeGoals() {
        CreatePlayerRequest request = createValidRequest();
        request.setGoals(-1);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("goals"));
    }

    @Test
    @DisplayName("Should pass when goals is zero")
    void zeroGoals() {
        CreatePlayerRequest request = createValidRequest();
        request.setGoals(0);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("goals"));
    }

    @Test
    @DisplayName("Should fail when goals is null")
    void nullGoals() {
        CreatePlayerRequest request = createValidRequest();
        request.setGoals(null);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("goals"));
    }

    @Test
    @DisplayName("Should fail when position is null")
    void nullPosition() {
        CreatePlayerRequest request = createValidRequest();
        request.setPosition(null);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("position"));
    }

    @Test
    @DisplayName("Should fail when age is below minimum")
    void ageBelowMinimum() {
        CreatePlayerRequest request = createValidRequest();
        request.setAge(14);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should pass when age is exactly minimum")
    void ageAtMinimum() {
        CreatePlayerRequest request = createValidRequest();
        request.setAge(15);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should fail when age is null")
    void nullAge() {
        CreatePlayerRequest request = createValidRequest();
        request.setAge(null);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should fail when assists is negative")
    void negativeAssists() {
        CreatePlayerRequest request = createValidRequest();
        request.setAssists(-1);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("assists"));
    }

    @Test
    @DisplayName("Should fail when matches is negative")
    void negativeMatches() {
        CreatePlayerRequest request = createValidRequest();
        request.setMatches(-1);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("matches"));
    }

    @Test
    @DisplayName("Should fail when valueMarket is negative")
    void negativeValueMarket() {
        CreatePlayerRequest request = createValidRequest();
        request.setValueMarket(-1);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("valueMarket"));
    }

    @Test
    @DisplayName("Should fail when teamName is blank")
    void blankTeamName() {
        CreatePlayerRequest request = createValidRequest();
        request.setTeamName("");

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    @DisplayName("Should fail when teamName is null")
    void nullTeamName() {
        CreatePlayerRequest request = createValidRequest();
        request.setTeamName(null);

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    @DisplayName("Should accumulate multiple violations")
    void multipleViolations() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName("");
        request.setGoals(-5);
        request.setPosition(null);
        request.setAge(10);
        request.setAssists(-1);
        request.setMatches(-1);
        request.setValueMarket(-1);
        request.setTeamName("");

        Set<ConstraintViolation<CreatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).hasSizeGreaterThanOrEqualTo(8);
    }
}
