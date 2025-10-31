# Publishing Guide

## Prerequisites

1. Create an npm account at https://www.npmjs.com/
2. Login to npm:
   ```bash
   npm login
   ```
3. Verify you're logged in:
   ```bash
   npm whoami
   ```

## Publishing Steps

### 1. Update Package Version

Edit `package.json` and update the version number following [Semantic Versioning](https://semver.org/):
- MAJOR version for incompatible API changes
- MINOR version for new functionality in a backwards compatible manner
- PATCH version for backwards compatible bug fixes

```json
{
  "version": "1.0.0"
}
```

### 2. Update Changelog

Add release notes to `CHANGELOG.md` documenting all changes.

### 3. Test Locally

Before publishing, test the package locally:

```bash
# In the package directory
npm pack
```

This creates a `.tgz` file. Test it in your app:

```bash
# In your React Native app
npm install ../react-native-video-transformer/sanjeevkumarrao-react-native-video-transformer-1.0.0.tgz
```

### 4. Publish to npm

```bash
# In the package directory
npm publish --access public
```

Note: Use `--access public` for scoped packages (@sanjeevkumarrao/...)

### 5. Create Git Tag

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 6. Create GitHub Release

1. Go to your GitHub repository
2. Click "Releases" → "Create a new release"
3. Select the tag you just created
4. Add release notes from CHANGELOG.md
5. Publish the release

## Updating the Package

When releasing updates:

1. Make your changes
2. Update version in `package.json`
3. Update `CHANGELOG.md`
4. Commit changes:
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   ```
5. Create git tag:
   ```bash
   git tag v1.0.1
   git push origin main
   git push origin v1.0.1
   ```
6. Publish to npm:
   ```bash
   npm publish
   ```

## Unpublishing (Use with Caution!)

You can unpublish within 72 hours of publishing:

```bash
npm unpublish @sanjeevkumarrao/react-native-video-transformer@1.0.0
```

**Warning**: Unpublishing is discouraged and may break dependent packages. Only use for critical issues.

## Best Practices

1. **Test thoroughly** before publishing
2. **Follow semver** strictly
3. **Document breaking changes** clearly
4. **Maintain changelog** with every release
5. **Don't publish** directly to main branch in production
6. **Use CI/CD** for automated testing and publishing
7. **Add badges** to README (npm version, downloads, etc.)

## Helpful Commands

```bash
# Check what files will be included in the package
npm pack --dry-run

# View package info
npm view @sanjeevkumarrao/react-native-video-transformer

# Check package size
npm pack && tar -xvzf *.tgz && du -sh package

# Link locally for testing
npm link
# Then in your app:
npm link @sanjeevkumarrao/react-native-video-transformer
```

## Troubleshooting

### "You do not have permission to publish"

Make sure you're logged in and the package name is unique or you own the scope.

### "Package name too similar to existing package"

Choose a more unique name or use a scoped package (@yourname/package-name).

### "Version already exists"

You must increment the version number before publishing again.
