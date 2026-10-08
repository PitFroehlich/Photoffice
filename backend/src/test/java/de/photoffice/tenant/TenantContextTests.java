package de.photoffice.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantContextTests {

	private final TenantId studioA = TenantId.of(UUID.randomUUID());

	private final TenantId studioB = TenantId.of(UUID.randomUUID());

	@Test
	void isEmptyOutsideOfABinding() {
		assertThat(TenantContext.current()).isEmpty();
		assertThatIllegalStateException().isThrownBy(TenantContext::require);
	}

	@Test
	void bindsTenantOnlyForTheGivenAction() {
		TenantContext.runAs(studioA, () -> assertThat(TenantContext.require()).isEqualTo(studioA));
		assertThat(TenantContext.current()).isEmpty();
	}

	@Test
	void innerBindingWinsAndOuterIsRestored() {
		TenantContext.runAs(studioA, () -> {
			TenantContext.runAs(studioB, () -> assertThat(TenantContext.require()).isEqualTo(studioB));
			assertThat(TenantContext.require()).isEqualTo(studioA);
		});
	}

	@Test
	void returnsResultOfCallable() {
		TenantId result = TenantContext.callAs(studioA, TenantContext::require);
		assertThat(result).isEqualTo(studioA);
	}

}
