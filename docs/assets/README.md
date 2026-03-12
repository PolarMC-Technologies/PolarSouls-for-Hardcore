# Documentation Assets

This directory contains custom assets for the PolarSouls documentation site.

## CSS Styling

The `css/style.scss` file provides comprehensive custom styling for the Jekyll documentation site:

### Features Included:

- **Typography**: Modern font stack with improved readability
- **Color Scheme**: Coordinated color variables matching project branding
- **Code Blocks**: Enhanced syntax highlighting with proper borders and padding
- **Tables**: Styled with gradient headers, hover effects, and alternating row colors
- **Responsive Design**: Mobile-friendly layouts with appropriate breakpoints
- **Accessibility**: Focus indicators, skip-to-content links, and proper contrast
- **Navigation**: Enhanced header and button styles
- **Content Elements**: Styled blockquotes, callouts, badges, and images
- **Print Styles**: Optimized layout for printing documentation

### Technical Details:

- Uses Jekyll/Sass preprocessing with YAML frontmatter
- Imports the Cayman theme base styles via `@import "{{ site.theme }}"`
- CSS custom properties (variables) for easy theme customization
- Mobile-first responsive design approach
- Smooth scrolling and modern browser features

### Maintenance:

To modify the documentation styling:
1. Edit `/docs/assets/css/style.scss`
2. Commit and push changes to GitHub
3. GitHub Pages will automatically rebuild the site with the new styles

The CSS will be automatically processed by Jekyll when the site is built on GitHub Pages.
