import { Marked, Tokens } from 'marked';

const escapes: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (char) => escapes[char]);
}

/**
 * Markdown → HTML for texts written by studios (legal texts, #20). GitHub-flavoured Markdown with single line breaks
 * kept (addresses in the imprint). Raw HTML in the text is shown as text, never interpreted; images are shown as
 * their alt text (no external requests from customer pages). The result still has to go through Angular's
 * sanitizer (MarkdownView does that) – e.g. for `javascript:` links.
 */
const markdown = new Marked({
  gfm: true,
  breaks: true,
  async: false,
  renderer: {
    html: ({ text }: Tokens.HTML | Tokens.Tag) => escapeHtml(text),
    image: ({ text }: Tokens.Image) => escapeHtml(text),
  },
});

export function renderMarkdown(source: string): string {
  return markdown.parse(source, { async: false });
}
