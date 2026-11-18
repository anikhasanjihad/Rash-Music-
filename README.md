<p align="center">
  <img src="Banner.png" alt="Rash Music" width="100%" />
</p>

# Rash Music

A fast, smooth music player for Android — streaming, offline downloads, synced lyrics, a powerful equalizer, Wear OS remote and more.

**Developed by AHR** · [Telegram](https://t.me/+-Bo6KSNJWf9iNjQ)

## Install & update

Download the latest APK from the **Releases** page. Pick `RashMusic-vX.Y.Z.apk` (works on every phone, 32-bit included), or the build for your CPU (`_arm64-v8a`, `_armeabi-v7a`, `_x86_64`).

Updates are built in: when a newer release is published here, the app shows an update popup and installs it in place — no browser needed.

## Releasing a new version

1. Bump `bitchordVersionCode` and `bitchordVersionName` in `build.gradle.kts` (e.g. `2` / `"1.0.1"`).
2. Commit and push, then create and push a tag that matches: `git tag v1.0.1 && git push origin v1.0.1`.
3. The **Release APK** workflow (`docs/release-workflow.yml` — copy it to `.github/workflows/release.yml` once) builds signed APKs and publishes the GitHub Release. Phones with an older version will see the update popup.

### One-time setup: signing key

Every release must be signed with the **same** key, or phones will refuse to install it over the old one. Create it once:

```bash
keytool -genkeypair -v -keystore rashmusic-release.jks -alias rashmusic \
  -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 rashmusic-release.jks   # copy the output
```

Then add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the base64 output above |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `rashmusic` |
| `KEY_PASSWORD` | key password |

Keep a backup of `rashmusic-release.jks` somewhere safe — lose it and existing users can't update.

## Brand assets

`brand/` holds the logo and the animated splash as SVG. `tools/brand/gen_assets.py` regenerates every launcher icon, in-app logo, notification icon and TV banner from the same artwork.

## Credits & license

Rash Music is a derivative of [BitChord](https://github.com/kushagrasinghx/BitChord) by Kushagra Singh and is released under the same license, the **GNU General Public License v3.0** (see `LICENSE`). Original copyright notices in the source files are kept as required by the license. Third-party components keep their own licenses as noted in the files they live in.
