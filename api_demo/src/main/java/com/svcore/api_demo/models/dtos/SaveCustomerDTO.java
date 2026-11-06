package com.svcore.api_demo.models.dtos;

import com.svcore.api_demo.models.enums.TaxpayerType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCustomerDTO {

	@NotBlank(message = "El nombre o razón social es obligatorio")
	@Size(max = 150)
	private String name;

	@NotNull(message = "Indica si el cliente es contribuyente")
	private TaxpayerType taxpayerType;

	@Pattern(regexp = "^\\d{4}-\\d{6}-\\d{3}-\\d$", message = "El NIT debe tener el formato 0000-000000-000-0")
	private String nit;

	@Pattern(regexp = "^\\d{1,6}-\\d$", message = "El NRC debe tener el formato 000000-0")
	private String nrc;

	@Email(message = "El correo no es válido")
	@Size(max = 100)
	private String email;

	@Size(max = 200)
	private String address;

	/** Código de actividad económica del catálogo de Hacienda (lo exige el Crédito Fiscal). */
	@Pattern(regexp = "^\\d{5,6}$", message = "El código de actividad económica debe tener 5 o 6 dígitos")
	private String economicActivityCode;

	@Size(max = 150)
	private String economicActivity;

	@Pattern(regexp = "^[0-9+\\- ]{8,30}$", message = "El teléfono no es válido")
	private String phone;

	@Pattern(regexp = "^\\d{2}$", message = "El código de departamento debe tener 2 dígitos")
	private String departmentCode;

	@Pattern(regexp = "^\\d{2}$", message = "El código de municipio debe tener 2 dígitos")
	private String municipalityCode;
}
