package futbol.api.com.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LigaData {
    @JsonProperty("liga_id")
    private String ligaId;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("pais")
    private String pais;

    @JsonProperty("equipos")
    private List<EquipoData> equipos;

    @JsonProperty("estadisticas_jugadores")
    private List<JugadorStats> estadisticasJugadores;
}
