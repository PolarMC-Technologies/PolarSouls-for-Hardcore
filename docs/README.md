# PolarSouls Documentation

This directory contains the source files for the PolarSouls documentation website, built with Jekyll and hosted on GitHub Pages.

## Documentation Structure

- **index.md** - Home page with overview and navigation
- **quick-start.md** - 8-step quick start guide
- **installation.md** - Comprehensive installation guide
- **configuration.md** - Complete configuration reference
- **commands.md** - All commands with examples
- **revival-system.md** - HRM features and revival mechanics
- **troubleshooting.md** - Solutions to common issues
- **faq.md** - Frequently asked questions

## Building Locally

To preview the documentation locally:

1. Install Ruby and Bundler:
   ```bash
   gem install bundler
   ```

2. Install dependencies:
   ```bash
   cd docs
   bundle install
   ```

3. Serve the site locally:
   ```bash
   bundle exec jekyll serve
   ```

4. Open your browser to `http://localhost:4000/PolarSouls-for-Hardcore/`

## Deployment

The documentation is automatically deployed to GitHub Pages when changes are pushed to the `main` branch. The deployment is handled by the `.github/workflows/deploy-docs.yml` workflow.

**Live URL:** https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/

## Theme

The documentation uses the [Cayman theme](https://github.com/pages-themes/cayman) for GitHub Pages with the following plugins:
- jekyll-seo-tag
- jekyll-sitemap

## Contributing

To contribute to the documentation:

1. Edit the relevant `.md` files in this directory
2. Test your changes locally using `bundle exec jekyll serve`
3. Submit a pull request with your changes

Please ensure:
- All pages have proper YAML front matter
- Navigation links are updated if adding new pages
- Code examples are properly formatted
- Internal links work correctly

## File Organization

```
docs/
├── _config.yml           # Jekyll configuration
├── Gemfile              # Ruby dependencies
├── index.md             # Home page
├── quick-start.md       # Quick start guide
├── installation.md      # Installation guide
├── configuration.md     # Configuration reference
├── commands.md          # Commands reference
├── revival-system.md    # Revival system guide
├── troubleshooting.md   # Troubleshooting guide
└── faq.md              # FAQ page
```

## Updating Documentation

When updating the documentation:

1. Keep the language clear and concise
2. Use code blocks for configuration examples
3. Include navigation links at the bottom of pages
4. Update the table of contents if structure changes
5. Test all code examples to ensure they work
6. Maintain consistency with existing formatting

## Questions?

For questions about the documentation:
- Open an issue on GitHub
- Check existing issues for similar questions
- Contact the maintainers
