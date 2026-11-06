package com.svcore.api_demo.models.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "rol")
public class Rol {

	/** Código de 4 letras: ADMN, VEND, INVE. Es la autoridad que se valida en la seguridad. */
	@Id
	@EqualsAndHashCode.Include
	@Column(name = "id", length = 4)
	private String id;

	@Column(name = "name", nullable = false, length = 50)
	private String name;
}
