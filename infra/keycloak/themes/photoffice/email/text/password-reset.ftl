<#ftl output_format="plainText">
${msg("passwordResetTitle")}

<#if user?? && user.firstName?has_content>${msg("photofficeGreetingName", user.firstName, user.lastName!"")}<#else>${msg("photofficeGreeting")}</#if>

${msg("passwordResetIntro")}

${link}

${msg("photofficeLinkExpiration", linkExpirationFormatter(linkExpiration))}

${msg("passwordResetIgnore")}

--
${msg("photofficeFooter")}
