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

class UpdatePlayerRequestTest {

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
        UpdatePlayerRequest request = new UpdatePlayerRequest();

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when age is below minimum")
    void ageBelowMinimum() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setAge(14);

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should pass when age is exactly 15")
    void ageAtMinimum() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setAge(15);

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should pass when age is null (not updating age)")
    void nullAge() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setAge(null);

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("Should fail when name is too short")
    void nameTooShort() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("A");

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should pass with valid name length")
    void validName() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("Lionel Messi");

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass with null name (partial update)")
    void nullName() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName(null);

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when name is blank spaces")
    void blankName() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("   ");

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when name is longer than create contract")
    void nameTooLong() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("a".repeat(51));

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should pass when name matches create contract maximum")
    void nameAtCreateContractMaximum() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("a".repeat(50));

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Should fail when teamName is blank")
    void blankTeamName() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamName("\t \n");

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    @DisplayName("Should fail when teamName exceeds create contract maximum")
    void teamNameTooLong() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamName("a".repeat(101));

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    @DisplayName("Should pass when teamName matches create contract maximum")
    void teamNameAtCreateContractMaximum() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamName("a".repeat(100));

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("teamName"));
    }

    @Test
    @DisplayName("Should pass with all valid fields")
    void allFieldsValid() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("Cristiano Ronaldo");
        request.setGoals(25);
        request.setPosition(Position.FORWARD);
        request.setAge(33);
        request.setAssists(10);
        request.setMatches(30);
        request.setValueMarket(80_000_000);
        request.setTeamName("Juventus");

        Set<ConstraintViolation<UpdatePlayerRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
