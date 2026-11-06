package com.svcore.api_demo.models.dtos;

import java.time.LocalDateTime;

import com.svcore.api_demo.models.entities.EmitterConfig;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CU-09: datos fiscales del emisor (lectura y actualización). */
@Data
@NoArgsConstructor
public class EmitterConfigDTO {

	@NotBlank @Size(max = 250)
	private String businessName;

	@NotBlank @Size(max = 150)
	private String tradeName;

	@NotBlank
	@Pattern(regexp = "^\\d{4}-\\d{6}-\\d{3}-\\d$", message = "El NIT debe tener el formato 0000-000000-000-0")
	private String nit;

	@NotBlank
	@Pattern(regexp = "^\\d{1,6}-\\d$", message = "El NRC debe tener el formato 000000-0")
	private String nrc;

	@NotBlank
	@Pattern(regexp = "^\\d{5,6}$", message = "El código de actividad económica debe tener 5 o 6 dígitos")
	private String economicActivityCode;

	@NotBlank @Size(max = 150)
	private String economicActivity;

	@NotBlank
	@Pattern(regexp = "^(01|02|04|07|20)$", message = "Tipo de establecimiento no válido (01, 02, 04, 07 o 20)")
	private String establishmentType;

	@NotBlank @Size(max = 100)
	private String establishmentName;

	@NotBlank
	@Pattern(regexp = "^\\d{2}$", message = "El código de departamento debe tener 2 dígitos")
	private String departmentCode;

	@NotBlank
	@Pattern(regexp = "^\\d{2}$", message = "El código de municipio debe tener 2 dígitos")
	private String municipalityCode;

	@NotBlank @Size(max = 200)
	private String addressDetail;

	@NotBlank
	@Pattern(regexp = "^[0-9+\\- ]{8,30}$", message = "El teléfono no es válido")
	private String phone;

	@NotBlank @Email @Size(max = 100)
	private String email;

	@NotBlank
	@Pattern(regexp = "^[A-Z0-9]{4}$", message = "El código de establecimiento debe tener 4 caracteres")
	private String establishmentCode;

	@NotBlank
	@Pattern(regexp = "^[A-Z0-9]{4}$", message = "El código de punto de venta debe tener 4 caracteres")
	private String pointOfSaleCode;

	@NotBlank @Size(max = 50)
	private String cashRegister;

	@NotBlank
	@Pattern(regexp = "^(00|01)$", message = "El ambiente debe ser 00 (pruebas) o 01 (producción)")
	private String environment;

	private LocalDateTime updatedAt;

	public static EmitterConfigDTO from(EmitterConfig c) {
		EmitterConfigDTO dto = new EmitterConfigDTO();
		dto.businessName = c.getBusinessName();
		dto.tradeName = c.getTradeName();
		dto.nit = c.getNit();
		dto.nrc = c.getNrc();
		dto.economicActivityCode = c.getEconomicActivityCode();
		dto.economicActivity = c.getEconomicActivity();
		dto.establishmentType = c.getEstablishmentType();
		dto.establishmentName = c.getEstablishmentName();
		dto.departmentCode = c.getDepartmentCode();
		dto.municipalityCode = c.getMunicipalityCode();
		dto.addressDetail = c.getAddressDetail();
		dto.phone = c.getPhone();
		dto.email = c.getEmail();
		dto.establishmentCode = c.getEstablishmentCode();
		dto.pointOfSaleCode = c.getPointOfSaleCode();
		dto.cashRegister = c.getCashRegister();
		dto.environment = c.getEnvironment();
		dto.updatedAt = c.getUpdatedAt();
		return dto;
	}
}
