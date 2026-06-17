package futbol.api.com.controllers;

import futbol.api.com.exceptions.GlobalExceptionHandler;
import futbol.api.com.services.Player.PlayerService;
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

class PlayerControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PlayerService playerService = mock(PlayerService.class);
        PlayerController controller = new PlayerController(playerService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("PUT /futbix/v1/players/{id} with blank name -> 400")
    void updatePlayer_blankName_returnsBadRequest() throws Exception {
        UUID playerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(put("/futbix/v1/players/{id}", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("PUT /futbix/v1/players/{id} with blank teamName -> 400")
    void updatePlayer_blankTeamName_returnsBadRequest() throws Exception {
        UUID playerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        mockMvc.perform(put("/futbix/v1/players/{id}", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("teamName"));
    }

    @Test
    @DisplayName("PUT /futbix/v1/players/{id} with name longer than create contract -> 400")
    void updatePlayer_nameLongerThanCreateContract_returnsBadRequest() throws Exception {
        UUID playerId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        mockMvc.perform(put("/futbix/v1/players/{id}", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
}
