package de.photoffice.identity;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps Keycloak realm roles ({@code realm_access.roles}) to {@code ROLE_*} authorities.
 */
class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		if (!(jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess)
				|| !(realmAccess.get("roles") instanceof Collection<?> roles)) {
			return List.of();
		}
		return roles.stream()
			.map(String::valueOf)
			.map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
					"ROLE_" + role.toUpperCase(Locale.ROOT).replace('-', '_')))
			.toList();
	}

}
