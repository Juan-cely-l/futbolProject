package futbol.api.com.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "age", "team_id"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private Integer goals;

    @Enumerated(EnumType.STRING)
    private Position position;

    private Integer age;

    private Integer assists;

    private Integer matches;

    private Integer valueMarket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
}
