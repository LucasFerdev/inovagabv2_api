package br.com.inovagab.api.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.inovagab.api.dto.RespostaErro;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RespostaErro> tratarValidacao(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		String mensagem = exception.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.distinct()
				.collect(Collectors.joining("; "));
		return resposta(HttpStatus.BAD_REQUEST, mensagem, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<RespostaErro> tratarCorpoInvalido(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request);
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ResponseEntity<RespostaErro> tratarCredenciais(CredenciaisInvalidasException exception,
			HttpServletRequest request) {
		return resposta(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<RespostaErro> tratarAcessoNegado(AccessDeniedException exception,
			HttpServletRequest request) {
		return resposta(HttpStatus.FORBIDDEN, "Acesso negado", request);
	}

	@ExceptionHandler(UsuarioInativoException.class)
	public ResponseEntity<RespostaErro> tratarUsuarioInativo(UsuarioInativoException exception,
			HttpServletRequest request) {
		return resposta(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler({ EmailDuplicadoException.class, DuplicateKeyException.class })
	public ResponseEntity<RespostaErro> tratarEmailDuplicado(Exception exception, HttpServletRequest request) {
		return resposta(HttpStatus.CONFLICT, "Já existe um usuário cadastrado com este e-mail", request);
	}

	@ExceptionHandler(UsuarioNaoEncontradoException.class)
	public ResponseEntity<RespostaErro> tratarUsuarioNaoEncontrado(UsuarioNaoEncontradoException exception,
			HttpServletRequest request) {
		return resposta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<RespostaErro> tratarErroInesperado(Exception exception, HttpServletRequest request) {
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado", request);
	}

	private ResponseEntity<RespostaErro> resposta(HttpStatus status, String mensagem, HttpServletRequest request) {
		RespostaErro erro = new RespostaErro(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				mensagem,
				request.getRequestURI());
		return ResponseEntity.status(status).body(erro);
	}
}
