<#-- "Passwort vergessen?" on the login page (issue #36). -->
<#import "template.ftl" as layout>
<@layout.emailLayout>
<h1 style="margin:16px 0 0 0; font-size:20px; font-weight:600; color:#1f1b17;">${msg("passwordResetTitle")}</h1>
<@layout.greeting/>
<p style="margin:16px 0;">${msg("passwordResetIntro")}</p>
<@layout.button href=link label=msg("passwordResetButton")/>
<p style="margin:16px 0;">${msg("photofficeLinkExpiration", linkExpirationFormatter(linkExpiration))}</p>
<p style="margin:16px 0; color:#4b4641;">${msg("passwordResetIgnore")}</p>
</@layout.emailLayout>
