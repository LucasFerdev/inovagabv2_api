package br.com.inovagab.api.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.inovagab.api.model.Usuario;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public String gerarToken(Usuario usuario) {
		Instant emitidoEm = Instant.now();
		Instant expiraEm = emitidoEm.plusSeconds(properties.expirationSeconds());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(emitidoEm)
				.expiresAt(expiraEm)
				.subject(usuario.getId())
				.claim("email", usuario.getEmail())
				.claim("role", usuario.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long getExpirationSeconds() {
		return properties.expirationSeconds();
	}
}
