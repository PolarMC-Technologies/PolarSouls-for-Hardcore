# PolarSouls Documentation

This directory contains the source files for the PolarSouls documentation website, hosted on GitHub Pages.

## Documentation Structure

The documentation is a **Jekyll site** using the `jekyll-theme-cayman` theme with Markdown pages.

- **index.md** - Documentation home page and entry point
- **quick-start.md** - Quick setup guide
- **installation.md** - Full installation and network setup
- **configuration.md** - Configuration reference
- **commands.md** - Command list and permissions
- **revival-system.md** - Revival mechanics and structures
- **troubleshooting.md** - Common problems and fixes
- **faq.md** - Frequently asked questions
- **assets/css/style.scss** - Lightweight Cayman theme overrides

## Viewing Locally

To preview with Jekyll locally:

1. `cd docs`
2. `bundle install`
3. `bundle exec jekyll serve`
4. Open the local URL shown in the terminal (usually `http://127.0.0.1:4000/PolarSouls-for-Hardcore/`)

## Deployment

The documentation is automatically deployed to GitHub Pages when changes are pushed to the `main` branch. The deployment is handled by the `.github/workflows/deploy-docs.yml` workflow.

**Live URL:** https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/

## Contributing

To contribute to the documentation:

1. Edit the relevant Markdown page in `docs/*.md`
2. Adjust `assets/css/style.scss` only when visual overrides are needed
3. Preview with `bundle exec jekyll serve`
3. Submit a pull request with your changes

Please ensure:
- Internal links between pages remain valid (`quick-start`, `installation`, etc.)
- Code examples are properly formatted
- The site renders correctly on desktop and mobile widths

## Questions?

For questions about the documentation:
- Open an issue on GitHub
- Check existing issues for similar questions
- Contact the maintainers
