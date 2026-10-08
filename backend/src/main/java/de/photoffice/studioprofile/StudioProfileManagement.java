package de.photoffice.studioprofile;

import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.TenantId;
import de.photoffice.tenant.TenantManagement;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Studio profile and legal texts of the current studio ({@link TenantContext}). Public API of the module, also the
 * read model for the customer gallery view (#12) and the checkout (#14).
 */
@Service
@Transactional
public class StudioProfileManagement {

	private final StudioProfileRepository profiles;

	private final LegalTextRepository legalTexts;

	private final TenantManagement tenants;

	private final Clock clock;

	StudioProfileManagement(StudioProfileRepository profiles, LegalTextRepository legalTexts,
			TenantManagement tenants, Optional<Clock> clock) {
		this.profiles = profiles;
		this.legalTexts = legalTexts;
		this.tenants = tenants;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	/** The saved profile, or an empty one with the studio name if the studio has not saved a profile yet. */
	@Transactional(readOnly = true)
	public StudioProfile profile() {
		TenantId tenant = TenantContext.require();
		return profiles.findById(tenant.value()).orElseGet(() -> StudioProfile.empty(tenant, studioName(tenant)));
	}

	public StudioProfile update(StudioProfileData data) {
		TenantId tenant = TenantContext.require();
		StudioProfile profile = profiles.findById(tenant.value())
			.orElseGet(() -> StudioProfile.empty(tenant, studioName(tenant)));
		profile.apply(data, now());
		return profiles.saveAndFlush(profile);
	}

	/** All legal texts in display order; texts not written yet are empty. */
	@Transactional(readOnly = true)
	public List<LegalTextContent> legalTexts() {
		Map<LegalTextKind, LegalText> saved = legalTexts.findAllBy()
			.stream()
			.collect(Collectors.toMap(LegalText::kind, Function.identity()));
		return Arrays.stream(LegalTextKind.values()).map(kind -> toContent(kind, saved.get(kind))).toList();
	}

	@Transactional(readOnly = true)
	public LegalTextContent legalText(LegalTextKind kind) {
		return toContent(kind, legalTexts.findByKind(kind).orElse(null));
	}

	/** Saves the Markdown text; an empty text removes it. */
	public LegalTextContent changeLegalText(LegalTextKind kind, String markdown) {
		String normalized = LegalText.normalize(markdown);
		Optional<LegalText> existing = legalTexts.findByKind(kind);
		if (normalized == null) {
			existing.ifPresent(text -> {
				legalTexts.delete(text);
				legalTexts.flush();
			});
			return toContent(kind, null);
		}
		LegalText text = existing.orElseGet(() -> new LegalText(TenantContext.require(), kind));
		text.change(normalized, now());
		return toContent(kind, legalTexts.saveAndFlush(text));
	}

	private String studioName(TenantId tenant) {
		return tenants.findById(tenant).map(Tenant::name).orElse("");
	}

	private static LegalTextContent toContent(LegalTextKind kind, LegalText text) {
		return text == null ? new LegalTextContent(kind, "", Optional.empty())
				: new LegalTextContent(kind, text.markdown(), Optional.of(text.updatedAt()));
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
