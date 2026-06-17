package futbol.api.com.controllers;

import futbol.api.com.exceptions.GlobalExceptionHandler;
import futbol.api.com.services.Team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TeamService teamService = mock(TeamService.class);
        TeamController controller = new TeamController(teamService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("PUT /futbix/v1/teams/{id} with blank name -> 400")
    void updateTeam_blankName_returnsBadRequest() throws Exception {
        UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(put("/futbix/v1/teams/{id}", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("PUT /futbix/v1/teams/{id} with blank city -> 400")
    void updateTeam_blankCity_returnsBadRequest() throws Exception {
        UUID teamId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(put("/futbix/v1/teams/{id}", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("city"));
    }

    @Test
    @DisplayName("PUT /futbix/v1/teams/{id} with name longer than create contract -> 400")
    void updateTeam_nameLongerThanCreateContract_returnsBadRequest() throws Exception {
        UUID teamId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(put("/futbix/v1/teams/{id}", teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
}
