<#ftl output_format="plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></#list></#if></#assign>
${msg("executeActionsTitle")}

<#if user?? && user.firstName?has_content>${msg("photofficeGreetingName", user.firstName, user.lastName!"")}<#else>${msg("photofficeGreeting")}</#if>

${msg("executeActionsIntro")}

${msg("executeActionsSteps", requiredActionsText)}

${link}

${msg("photofficeLinkExpiration", linkExpirationFormatter(linkExpiration))}

${msg("executeActionsIgnore")}

--
${msg("photofficeFooter")}
