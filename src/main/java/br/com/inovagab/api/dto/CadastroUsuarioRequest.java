package br.com.inovagab.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioRequest(
		@NotBlank(message = "O nome é obrigatório") String nome,
		@NotBlank(message = "O e-mail é obrigatório") @Email(message = "O e-mail deve ser válido") String email,
		@NotBlank(message = "A senha é obrigatória") @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
		@NotBlank(message = "A empresa é obrigatória") String empresa) {
}
