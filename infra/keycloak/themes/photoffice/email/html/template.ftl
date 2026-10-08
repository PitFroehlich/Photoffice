<#-- Photoffice layout for HTML e-mails (issue #36). Inline styles only: many mail clients ignore <style> blocks. -->
<#macro emailLayout>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="margin:0; padding:0; background-color:#f9efe8;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f9efe8;">
  <tr>
    <td align="center" style="padding:24px 12px;">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
             style="max-width:560px; background-color:#ffffff; border-top:4px solid #c8794a; border-radius:16px; font-family:'Inter Variable','Inter','Segoe UI',Helvetica,Arial,sans-serif; font-size:16px; line-height:1.5; color:#1f1b17;">
        <tr>
          <td style="padding:24px 32px 0 32px; font-size:22px; font-weight:600; color:#1f4e5f;">Photoffice</td>
        </tr>
        <tr>
          <td style="padding:8px 32px 32px 32px;">
<#nested>
          </td>
        </tr>
      </table>
      <p style="max-width:560px; margin:16px auto 0 auto; font-family:'Inter Variable','Inter','Segoe UI',Helvetica,Arial,sans-serif; font-size:12px; line-height:1.5; color:#4b4641;">${msg("photofficeFooter")}</p>
    </td>
  </tr>
</table>
</body>
</html>
</#macro>

<#-- Greeting with the user's name if known -->
<#macro greeting>
<p style="margin:16px 0;"><#if user?? && user.firstName?has_content>${msg("photofficeGreetingName", user.firstName, user.lastName!"")}<#else>${msg("photofficeGreeting")}</#if></p>
</#macro>

<#-- Main action as a petrol, rounded button (like the app's filled button) plus the plain link as fallback -->
<#macro button href label>
<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0;">
  <tr>
    <td style="border-radius:999px; background-color:#1f4e5f;">
      <a href="${href}" style="display:inline-block; padding:12px 28px; border-radius:999px; color:#ffffff; font-weight:600; text-decoration:none;">${label}</a>
    </td>
  </tr>
</table>
<p style="margin:16px 0; font-size:14px; color:#4b4641;">${msg("photofficeLinkFallback")}<br><a href="${href}" style="color:#1f4e5f; font-size:12px; word-break:break-all;">${href}</a></p>
</#macro>
