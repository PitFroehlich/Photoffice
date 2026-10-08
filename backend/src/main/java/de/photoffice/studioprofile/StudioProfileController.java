package de.photoffice.studioprofile;

import de.photoffice.api.StudioProfileApi;
import de.photoffice.api.model.LegalTextInput;
import de.photoffice.api.model.StudioProfileInput;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class StudioProfileController implements StudioProfileApi {

	private final StudioProfileManagement studioProfileManagement;

	StudioProfileController(StudioProfileManagement studioProfileManagement) {
		this.studioProfileManagement = studioProfileManagement;
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.StudioProfile> getStudioProfile() {
		return ResponseEntity.ok(toResponse(studioProfileManagement.profile()));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.StudioProfile> updateStudioProfile(StudioProfileInput input) {
		return ResponseEntity.ok(toResponse(studioProfileManagement.update(toData(input))));
	}

	@Override
	public ResponseEntity<List<de.photoffice.api.model.LegalText>> listLegalTexts() {
		return ResponseEntity
			.ok(studioProfileManagement.legalTexts().stream().map(StudioProfileController::toResponse).toList());
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.LegalText> updateLegalText(de.photoffice.api.model.LegalTextKind kind,
			LegalTextInput input) {
		LegalTextKind legalTextKind = LegalTextKind.valueOf(kind.getValue());
		return ResponseEntity.ok(toResponse(studioProfileManagement.changeLegalText(legalTextKind, input.getMarkdown())));
	}

	@ExceptionHandler
	ProblemDetail onInvalidInput(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	private static StudioProfileData toData(StudioProfileInput input) {
		Country country = input.getCountry() == null ? null : Country.valueOf(input.getCountry().getValue());
		return new StudioProfileData(input.getDisplayName(), input.getStreet(), input.getPostalCode(), input.getCity(),
				country, input.getEmail(), input.getPhone(), input.getWebsite(), input.getTaxNumber(), input.getVatId(),
				input.getAccountHolder(), input.getIban(), input.getBic());
	}

	private static de.photoffice.api.model.StudioProfile toResponse(StudioProfile profile) {
		var response = new de.photoffice.api.model.StudioProfile(profile.displayName(),
				de.photoffice.api.model.Country.fromValue(profile.country().name()));
		response.setStreet(profile.street());
		response.setPostalCode(profile.postalCode());
		response.setCity(profile.city());
		response.setEmail(profile.email());
		response.setPhone(profile.phone());
		response.setWebsite(profile.website());
		response.setTaxNumber(profile.taxNumber());
		response.setVatId(profile.vatId());
		response.setAccountHolder(profile.accountHolder());
		response.setIban(profile.iban());
		response.setBic(profile.bic());
		profile.updatedAt().ifPresent(updatedAt -> response.setUpdatedAt(updatedAt.atOffset(ZoneOffset.UTC)));
		return response;
	}

	private static de.photoffice.api.model.LegalText toResponse(LegalTextContent text) {
		var response = new de.photoffice.api.model.LegalText(
				de.photoffice.api.model.LegalTextKind.fromValue(text.kind().name()), text.markdown());
		text.updatedAt().ifPresent(updatedAt -> response.setUpdatedAt(updatedAt.atOffset(ZoneOffset.UTC)));
		return response;
	}

}
