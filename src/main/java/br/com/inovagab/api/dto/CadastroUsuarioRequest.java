package br.com.inovagab.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioRequest(
		@NotBlank(message = "O nome é obrigatório") String nome,
		@NotBlank(message = "O e-mail é obrigatório") @Email(message = "O e-mail deve ser válido") String email,
		@NotBlank(message = "A senha é obrigatória") @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
		@NotBlank(message = "A empresa é obrigatória") String empresa,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String codigoAcesso) {

	public CadastroUsuarioRequest(String nome, String email, String senha, String empresa) {
		this(nome, email, senha, empresa, null);
	}

	@Override
	public String toString() {
		return "CadastroUsuarioRequest[campos protegidos]";
	}
}
