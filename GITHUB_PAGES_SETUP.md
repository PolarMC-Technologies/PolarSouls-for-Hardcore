# GitHub Pages Setup Instructions

This guide explains how to enable GitHub Pages for the PolarSouls documentation.

## Prerequisites

- Repository owner/admin access
- Documentation files are in the `docs/` directory (✅ already done)
- GitHub Actions workflow is configured (✅ already done)

## Step 1: Enable GitHub Pages

1. Go to your repository on GitHub: https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore

2. Click on **Settings** (top navigation)

3. In the left sidebar, scroll down and click **Pages**

4. Under "Build and deployment":
   - **Source:** Select "GitHub Actions"
   - This will use the workflow we created in `.github/workflows/deploy-docs.yml`

5. Click **Save** (if applicable)

## Step 2: Trigger the First Deployment

The documentation will automatically deploy when you:
- Push changes to the `main` branch that affect the `docs/` directory
- Manually trigger the workflow

**To manually trigger the workflow:**

1. Go to **Actions** tab in your repository

2. Click on "Deploy Documentation to GitHub Pages" workflow

3. Click **Run workflow** → Select `main` branch → Click **Run workflow**

4. Wait for the workflow to complete (usually 1-2 minutes)

## Step 3: Verify Deployment

1. Go back to **Settings** → **Pages**

2. You should see a message: "Your site is live at https://ssoggytacoblud.github.io/PolarSouls-for-Hardcore/"

3. Click the link to view your documentation

4. Verify all pages load correctly:
   - Home page
   - Quick Start
   - Installation
   - Configuration
   - Commands
   - Revival System
   - Troubleshooting
   - FAQ

## Step 4: Update Links (if needed)

If your GitHub username is different from "SSoggyTacoBlud", update the following files:

### Update `docs/_config.yml`:
```yaml
url: "https://YOUR-USERNAME.github.io"
baseurl: "/PolarSouls-for-Hardcore"
```

### Update MODRINTH.md and README.md:
Replace all instances of:
```
https://ssoggytacoblud.github.io/PolarSouls-for-Hardcore/
```

With:
```
https://YOUR-USERNAME.github.io/PolarSouls-for-Hardcore/
```

## Troubleshooting

### Pages not enabled?

If GitHub Pages section doesn't show "GitHub Actions" as a source option:
1. Ensure you have the `deploy-docs.yml` workflow file
2. Push it to the `main` branch
3. Go to **Actions** → Check if the workflow appears
4. Enable Actions if disabled in repository settings

### Workflow fails?

Check the Actions tab for error messages:
- **Ruby/Jekyll errors:** Gemfile might need updates
- **Permissions errors:** Ensure workflow has correct permissions
- **Build errors:** Check Jekyll syntax in `.md` files

### 404 errors on documentation pages?

This usually means:
1. Jekyll build failed - check Actions logs
2. Incorrect `baseurl` in `_config.yml`
3. File names don't match links

### Site not updating?

1. Verify your changes were pushed to `main` branch
2. Check if the workflow ran successfully in Actions tab
3. Hard refresh your browser (Ctrl+Shift+R or Cmd+Shift+R)
4. Clear browser cache

## Customization

### Change Theme

Edit `docs/_config.yml`:
```yaml
theme: jekyll-theme-minimal  # or any other GitHub Pages theme
```

Available themes:
- jekyll-theme-cayman (current)
- jekyll-theme-minimal
- jekyll-theme-modernist
- jekyll-theme-slate
- jekyll-theme-architect
- jekyll-theme-time-machine

### Add Custom Domain

1. Purchase a domain name
2. Go to **Settings** → **Pages**
3. Under "Custom domain", enter your domain
4. Add DNS records as instructed by GitHub
5. Update `url` in `_config.yml` to your custom domain

## Maintenance

### Updating Documentation

1. Edit files in the `docs/` directory
2. Commit and push to `main` branch
3. GitHub Actions will automatically rebuild and deploy
4. Wait 1-2 minutes for changes to appear

### Adding New Pages

1. Create new `.md` file in `docs/` directory
2. Add YAML front matter:
   ```yaml
   ---
   layout: default
   title: Your Page Title
   ---
   ```
3. Add link to page in other documentation files
4. Update `docs/_config.yml` if needed for navigation
5. Commit and push

## Support

If you encounter issues:
1. Check [GitHub Pages documentation](https://docs.github.com/en/pages)
2. Check [Jekyll documentation](https://jekyllrb.com/docs/)
3. Review workflow logs in Actions tab
4. Open an issue with error details

## Next Steps

After enabling GitHub Pages:

1. ✅ Share the documentation URL with users
2. ✅ Update Modrinth plugin page with wiki link (if not already done)
3. ✅ Consider adding documentation badge to README
4. ✅ Monitor analytics (optional - can enable in GitHub Pages settings)

---

**Documentation URL:** https://ssoggytacoblud.github.io/PolarSouls-for-Hardcore/

**Last Updated:** 2026-02-13
