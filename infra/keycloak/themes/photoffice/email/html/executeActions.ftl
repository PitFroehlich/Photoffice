<#-- Sent by execute-actions-email. In Photoffice this is the invitation of a new studio admin (issue #24/#36). -->
<#outputformat "plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#sep></#items></#list></#if></#assign>
</#outputformat>

<#import "template.ftl" as layout>
<@layout.emailLayout>
<h1 style="margin:16px 0 0 0; font-size:20px; font-weight:600; color:#1f1b17;">${msg("executeActionsTitle")}</h1>
<@layout.greeting/>
<p style="margin:16px 0;">${msg("executeActionsIntro")}</p>
<p style="margin:16px 0;">${msg("executeActionsSteps", requiredActionsText)}</p>
<@layout.button href=link label=msg("executeActionsButton")/>
<p style="margin:16px 0;">${msg("photofficeLinkExpiration", linkExpirationFormatter(linkExpiration))}</p>
<p style="margin:16px 0; color:#4b4641;">${msg("executeActionsIgnore")}</p>
</@layout.emailLayout>
