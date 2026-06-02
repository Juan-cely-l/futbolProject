package futbol.api.com.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquipoData {
    @JsonProperty("equipo_id")
    private String equipoId;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("posicion_final")
    private int posicionFinal;

    @JsonProperty("puntos")
    private int puntos;

    @JsonProperty("pais")
    private String pais;

    @JsonProperty("clasificacion")
    private String clasificacion;
}
