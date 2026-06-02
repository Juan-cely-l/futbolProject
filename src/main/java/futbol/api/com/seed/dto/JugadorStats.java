package futbol.api.com.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JugadorStats {
    @JsonProperty("jugador_id")
    private String jugadorId;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("equipo_id")
    private String equipoId;

    @JsonProperty("posicion")
    private String posicion;

    @JsonProperty("goles")
    private int goles;

    @JsonProperty("asistencias")
    private int asistencias;

    @JsonProperty("partidos_jugados")
    private int partidosJugados;

    @JsonProperty("rol")
    private String rol;
}
