<#-- Copy of base/login/info.ftl (Keycloak 26.8) with two Photoffice changes (issue #36):
     a welcome heading when an administrator requested actions (invitation link), and the link as primary button. -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#elseif requiredActions??>
            ${msg("photofficeRequiredActionsTitle")}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction">${message.summary}<#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></b></#list><#else></#if></p>
        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <p><a class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" href="${pageRedirectUri}">${msg("backToApplication")}</a></p>
            <#elseif actionUri?has_content>
                <p><a class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" href="${actionUri}">${msg("proceedWithAction")}</a></p>
            <#elseif (client.baseUrl)?has_content>
                <p><a class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" href="${client.baseUrl}">${msg("backToApplication")}</a></p>
            </#if>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>
