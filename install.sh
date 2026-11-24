#!/usr/bin/env bash
# Kickoff CLI Installer
# Usage: curl -fsSL https://raw.githubusercontent.com/Andrer757/Kickoff-V2/main/install.sh | bash

set -euo pipefail

REPO="Andrer757/Kickoff-V2"
FALLBACK_VERSION="1.0.0"

# Colors (respect NO_COLOR and non-interactive stdout)
if [ -n "${NO_COLOR:-}" ] || [ ! -t 1 ]; then
    C_RESET=""
    C_BOLD=""
    C_GREEN=""
    C_CYAN=""
    C_YELLOW=""
    C_RED=""
else
    C_RESET="\033[0m"
    C_BOLD="\033[1m"
    C_GREEN="\033[32m"
    C_CYAN="\033[36m"
    C_YELLOW="\033[33m"
    C_RED="\033[31m"
fi

info() {
    printf "${C_CYAN}${C_BOLD}==>${C_RESET} ${C_BOLD}%s${C_RESET}\n" "$1"
}

success() {
    printf "${C_GREEN}${C_BOLD}✔${C_RESET} %s\n" "$1"
}

warn() {
    printf "${C_YELLOW}${C_BOLD}Warning:${C_RESET} %s\n" "$1"
}

error() {
    printf "${C_RED}${C_BOLD}Error:${C_RESET} %s\n" "$1" >&2
    exit 1
}

# Determine OS
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "$OS" in
    linux*)  PLATFORM="linux" ;;
    darwin*) PLATFORM="darwin" ;;
    *)       error "Unsupported operating system: $OS. Kickoff requires Linux or macOS." ;;
esac

# Resolve Version
VERSION="${KICKOFF_VERSION:-}"
if [ -z "$VERSION" ]; then
    info "Resolving latest Kickoff release..."
    LATEST_TAG=""
    if command -v curl >/dev/null 2>&1; then
        LATEST_TAG="$(curl -fsSL -H "Accept: application/vnd.github.v3+json" "https://api.github.com/repos/${REPO}/releases/latest" 2>/dev/null | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/' || true)"
    fi
    if [ -n "$LATEST_TAG" ]; then
        VERSION="${LATEST_TAG#v}"
    else
        VERSION="$FALLBACK_VERSION"
    fi
fi

TAG="v${VERSION#v}"
VERSION="${VERSION#v}"

info "Installing Kickoff CLI v${VERSION} (${PLATFORM})..."

# Directories
INSTALL_DIR="${KICKOFF_HOME:-$HOME/.kickoff}"
BIN_DIR="${KICKOFF_BIN_DIR:-$HOME/.local/bin}"

if [ "$(id -u)" -eq 0 ] && [ "$BIN_DIR" = "$HOME/.local/bin" ]; then
    BIN_DIR="/usr/local/bin"
fi

mkdir -p "$INSTALL_DIR"
mkdir -p "$BIN_DIR"

TAR_NAME="kickoff-cli-${VERSION}.tar"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${TAG}/${TAR_NAME}"
CHECKSUMS_URL="https://github.com/${REPO}/releases/download/${TAG}/checksums.txt"

TMP_DIR="$(mktemp -d -t kickoff-install.XXXXXX)"
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

download_file() {
    local url="$1"
    local dest="$2"
    if command -v curl >/dev/null 2>&1; then
        curl -fL --progress-bar -o "$dest" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget -q --show-progress -O "$dest" "$url"
    else
        error "Neither curl nor wget found in PATH."
    fi
}

info "Downloading ${DOWNLOAD_URL}..."
download_file "$DOWNLOAD_URL" "$TMP_DIR/$TAR_NAME" || error "Failed to download $DOWNLOAD_URL"

info "Verifying SHA-256 checksum..."
download_file "$CHECKSUMS_URL" "$TMP_DIR/checksums.txt" || error "Failed to download $CHECKSUMS_URL for integrity verification."
EXPECTED_SHA="$(awk -v file="$TAR_NAME" '$2 == file || $2 == "*" file { print $1; exit }' "$TMP_DIR/checksums.txt")"
if [ -z "$EXPECTED_SHA" ]; then
    error "No checksum entry found for ${TAR_NAME} in checksums.txt."
fi
if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL_SHA="$(sha256sum "$TMP_DIR/$TAR_NAME" | awk '{print $1}')"
elif command -v shasum >/dev/null 2>&1; then
    ACTUAL_SHA="$(shasum -a 256 "$TMP_DIR/$TAR_NAME" | awk '{print $1}')"
else
    error "Neither sha256sum nor shasum found in PATH to verify download integrity."
fi
if [ "$ACTUAL_SHA" != "$EXPECTED_SHA" ]; then
    error "Checksum mismatch for ${TAR_NAME} (expected ${EXPECTED_SHA}, got ${ACTUAL_SHA})."
fi
success "SHA-256 checksum verified"

info "Extracting Kickoff distribution..."
tar -xf "$TMP_DIR/$TAR_NAME" -C "$TMP_DIR"

EXTRACTED_DIR="$TMP_DIR/kickoff-cli-${VERSION}"
if [ ! -d "$EXTRACTED_DIR" ]; then
    error "Extracted directory $EXTRACTED_DIR not found in archive."
fi

# Atomic swap into INSTALL_DIR via sibling staging directory
if [ "$INSTALL_DIR" = "/" ]; then
    error "Refusing to install into root directory '/'."
fi
INSTALL_PARENT="$(dirname "$INSTALL_DIR")"
mkdir -p "$INSTALL_PARENT"
STAGING_DIR="$(mktemp -d "${INSTALL_PARENT}/.kickoff-staging.XXXXXX")"
BACKUP_DIR=""
cp -R "$EXTRACTED_DIR"/. "$STAGING_DIR/"
chmod +x "$STAGING_DIR/bin/kickoff-cli"

if [ -d "$INSTALL_DIR" ]; then
    BACKUP_DIR="${INSTALL_PARENT}/.kickoff-backup.$$"
    rm -rf "$BACKUP_DIR"
    mv "$INSTALL_DIR" "$BACKUP_DIR"
fi
if ! mv "$STAGING_DIR" "$INSTALL_DIR"; then
    if [ -n "$BACKUP_DIR" ] && [ -d "$BACKUP_DIR" ]; then
        mv "$BACKUP_DIR" "$INSTALL_DIR" || true
    fi
    rm -rf "$STAGING_DIR"
    error "Failed to move staged installation into $INSTALL_DIR."
fi
if [ -n "$BACKUP_DIR" ] && [ -d "$BACKUP_DIR" ]; then
    rm -rf "$BACKUP_DIR"
fi

# Create symlinks in BIN_DIR
ln -sf "$INSTALL_DIR/bin/kickoff-cli" "$BIN_DIR/kickoff"
ln -sf "$INSTALL_DIR/bin/kickoff-cli" "$BIN_DIR/kickoff-cli"

success "Kickoff CLI binaries linked to ${BIN_DIR}/kickoff"

# Java Environment Check
info "Checking Java environment..."
if command -v java >/dev/null 2>&1; then
    JAVA_VER_STR="$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | head -n 1)"
    JAVA_MAJOR="$(echo "$JAVA_VER_STR" | cut -d'.' -f1)"
    if [ "$JAVA_MAJOR" = "1" ]; then
        JAVA_MAJOR="$(echo "$JAVA_VER_STR" | cut -d'.' -f2)"
    fi
    if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -ge 21 ] && [ "$JAVA_MAJOR" -le 23 ] 2>/dev/null; then
        success "Java ${JAVA_VER_STR} detected (meets requirement JDK 21..23)"
    else
        warn "Detected Java version ${JAVA_VER_STR}. Kickoff requires JDK 21..23 (Temurin-21 recommended for Gradle 9.9.1 and Kotlin 2.4.21)."
    fi
else
    warn "No Java runtime found in PATH. Kickoff CLI requires JDK 21."
    printf "      Install JDK 21 via SDKMAN: ${C_CYAN}sdk install java 21-tem${C_RESET}\n"
    printf "      Or via Homebrew:          ${C_CYAN}brew install openjdk@21${C_RESET}\n"
fi

# PATH Check
PATH_OK=false
case ":$PATH:" in
    *":$BIN_DIR:"*) PATH_OK=true ;;
esac

echo ""
printf "${C_GREEN}${C_BOLD}======================================================${C_RESET}\n"
printf "${C_GREEN}${C_BOLD}   Kickoff CLI v${VERSION} Installed Successfully!   ${C_RESET}\n"
printf "${C_GREEN}${C_BOLD}======================================================${C_RESET}\n"
echo ""

if [ "$PATH_OK" = false ]; then
    printf "${C_YELLOW}${C_BOLD}Notice:${C_RESET} ${BIN_DIR} is not in your PATH.\n"
    printf "Add it to your shell configuration file:\n\n"
    printf "  ${C_CYAN}export PATH=\"%s:\$PATH\"${C_RESET}\n\n" "$BIN_DIR"
    printf "For Bash:  ${C_CYAN}echo 'export PATH=\"%s:\$PATH\"' >> ~/.bashrc${C_RESET}\n" "$BIN_DIR"
    printf "For Zsh:   ${C_CYAN}echo 'export PATH=\"%s:\$PATH\"' >> ~/.zshrc${C_RESET}\n" "$BIN_DIR"
    echo ""
fi

printf "Get started by scaffolding a new Android project:\n"
printf "  ${C_BOLD}kickoff new --name \"Crypto Tracker\" --package \"com.example.cryptotracker\"${C_RESET}\n\n"
printf "Or launch the AI MCP Server:\n"
printf "  ${C_BOLD}kickoff mcp${C_RESET}\n"
echo ""
