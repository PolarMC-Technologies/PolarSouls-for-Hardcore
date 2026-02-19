# PolarSouls Documentation

This directory contains the source files for the PolarSouls documentation website, hosted on GitHub Pages.

## Documentation Structure

The documentation is a **single-page HTML site** (`index.html`) with all content and navigation in one file.

- **index.html** - Complete documentation (overview, quick start, installation, configuration, commands, revival system, troubleshooting, FAQ)
- **styles.css** - All styling for the documentation site
- **assets/** - SVG icons and other static assets

## Viewing Locally

To preview the documentation locally, simply open `docs/index.html` in any browser — no build step required.

## Deployment

The documentation is automatically deployed to GitHub Pages when changes are pushed to the `main` branch. The deployment is handled by the `.github/workflows/deploy-docs.yml` workflow.

**Live URL:** https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/

## Contributing

To contribute to the documentation:

1. Edit `index.html` (content) or `styles.css` (styling) directly
2. Open `index.html` in a browser to verify your changes
3. Submit a pull request with your changes

Please ensure:
- Navigation anchors stay in sync with section IDs in `index.html`
- Code examples are properly formatted
- Both light and dark themes look correct (use the theme toggle button)

## Questions?

For questions about the documentation:
- Open an issue on GitHub
- Check existing issues for similar questions
- Contact the maintainers
