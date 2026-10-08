package de.photoffice.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Answers requests rejected by the {@code StrictHttpFirewall} (e.g. an empty path segment like
 * {@code /api/platform/tenants//suspend}) with a 400 problem detail. The default handler only sends the status as
 * container error; the error dispatch then ran without authentication into "deny all" – the client got an empty 401.
 */
class ProblemDetailRequestRejectedHandler implements RequestRejectedHandler {

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailRequestRejectedHandler.class);

	private static final String BODY = """
			{"type":"about:blank","title":"Bad Request","status":400,\
			"detail":"Die Anfrage-URL ist ungültig (z. B. ein leerer Pfadabschnitt wie „//“)."}""";

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			RequestRejectedException requestRejectedException) throws IOException {
		log.debug("Rejected request {}: {}", request.getRequestURI(), requestRejectedException.getMessage());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(BODY);
	}

}
